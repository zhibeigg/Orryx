package org.gitee.orryx.core.editor.release

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.gitee.orryx.core.editor.handler.EditorFileAllowlistDescriptor
import org.gitee.orryx.core.editor.handler.EditorFilePolicy
import org.gitee.orryx.core.editor.handler.ManifestEntryV1
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal interface ReleaseDownloader {
    suspend fun metadata(uri: java.net.URI, token: String, config: ReleaseConfig): ByteArray
    suspend fun file(uri: java.net.URI, token: String, config: ReleaseConfig, target: Path, expectedSize: Long): DownloadedFile
}

internal object OkHttpReleaseDownloader : ReleaseDownloader {
    override suspend fun metadata(uri: java.net.URI, token: String, config: ReleaseConfig): ByteArray =
        ReleaseHttpClient.fetchMetadata(uri, token, config)

    override suspend fun file(
        uri: java.net.URI,
        token: String,
        config: ReleaseConfig,
        target: Path,
        expectedSize: Long,
    ): DownloadedFile = ReleaseHttpClient.download(uri, token, config, target, expectedSize)
}

internal data class ReadinessReport(val success: Boolean, val message: String)

internal class ReleaseTransactionManager(
    private val liveRoot: Path,
    transactionsRoot: Path,
    private val configProvider: () -> ReleaseConfig,
    private val downloader: ReleaseDownloader = OkHttpReleaseDownloader,
    private val allowlist: EditorFileAllowlistDescriptor = EditorFileAllowlistDescriptor.ORRYX_CONFIG,
    private val checkpoint: (String, String) -> Unit = { _, _ -> },
) {
    private val store = ReleaseJournalStore(transactionsRoot)
    private val json = Json { ignoreUnknownKeys = false }
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    suspend fun prepare(request: ReleaseRequest): ReleaseResult = lock(request.transactionId) {
        val config = configProvider()
        if (!config.enabled) throw ReleaseException("RELEASE_DISABLED", "Editor.Release.Enable 未开启")
        val existing = store.load(request.transactionId)
        if (existing != null) {
            validateTransactionIdentity(existing, request)
            if (existing.prepareCommandId == request.commandId && existing.state != ReleaseState.PREPARING && existing.state != ReleaseState.FAILED) {
                return@lock emit(
                    existing.copy(commandId = request.commandId),
                    ReleaseAction.PREPARE,
                    successFor(existing.state),
                    message = "prepare 已处理",
                )
            }
            if (existing.state !in setOf(ReleaseState.PREPARING, ReleaseState.FAILED, ReleaseState.ROLLED_BACK)) {
                throw ReleaseException("TRANSACTION_ALREADY_EXISTS", "transactionId 已绑定其他 prepare command")
            }
        }

        val expectedRevision = requireNotNull(request.expectedManifestRevision)
        val targetRevision = requireNotNull(request.targetManifestRevision)
        val totalBytes = requireNotNull(request.totalBytes)
        val fileCount = requireNotNull(request.fileCount)
        val transferExpiresAt = requireNotNull(request.transferExpiresAt)
        val transferToken = requireNotNull(request.transferToken)
        val signature = requireNotNull(request.signature)
        val observed = livePolicy(config).snapshotManifest()
        if (observed.revision != expectedRevision) {
            throw ReleaseException("MANIFEST_PRECONDITION_FAILED", "当前 manifest 与 expectedManifestRevision 不一致")
        }
        if (totalBytes > config.maxStagedBytes) {
            throw ReleaseException("STAGED_BYTES_EXCEEDED", "发布总大小超过 MaxStagedBytes")
        }
        val now = System.currentTimeMillis()
        if (now - config.acceptedClockSkewMillis > transferExpiresAt) {
            throw ReleaseException("TRANSFER_TOKEN_EXPIRED", "transferToken 已过期")
        }

        store.createLayout(request.transactionId)
        store.deleteTree(store.stageDir(request.transactionId))
        store.deleteTree(store.backupDir(request.transactionId))
        Files.createDirectories(store.stageDir(request.transactionId))
        Files.createDirectories(store.backupDir(request.transactionId))
        var journal = ReleaseJournal(
            transactionId = request.transactionId,
            releaseId = request.releaseId,
            commandId = request.commandId,
            prepareCommandId = request.commandId,
            state = ReleaseState.PREPARING,
            canonicalPayloadSha256 = requireNotNull(request.canonicalPayloadSha256),
            signingKeyId = requireNotNull(request.signingKeyId),
            expectedManifestRevision = expectedRevision,
            targetManifestRevision = targetRevision,
            fileCount = fileCount,
            totalBytes = totalBytes,
            operationsUrl = requireNotNull(request.operationsUrl),
            transferExpiresAt = transferExpiresAt,
        )
        store.save(journal)

        try {
            val urlPolicy = ReleaseUrlPolicy(request.operationsUrl, config)
            val metadata = downloader.metadata(urlPolicy.operationsUri, transferToken, config)
            val operations = decodeOperations(metadata)
            validateOperationIdentity(operations, request, journal)
            val files = operations.files
            validateOperations(files, journal, observed.files, urlPolicy, config)
            buildFullStage(request, files, urlPolicy, config)
            val stageManifest = stagePolicy(request.transactionId, config).snapshotManifest()
            if (stageManifest.revision != targetRevision) {
                throw ReleaseException("TARGET_MANIFEST_MISMATCH", "stage manifest 与 targetManifestRevision 不一致")
            }
            val payload = ReleaseCanonical.Payload(
                keyId = operations.signingKeyId,
                releaseId = operations.releaseId,
                serverInstanceId = operations.serverInstanceId,
                stableServerId = operations.stableServerId,
                draftId = operations.draftId,
                draftVersionId = operations.draftVersionId,
                expectedBaseManifestRevision = operations.expectedManifestRevision,
                targetManifestRevision = operations.targetManifestRevision,
                createdAtEpochMillis = operations.createdAt,
                files = files,
            )
            val canonical = ReleaseCanonical.encode(payload)
            if (ReleaseCanonical.sha256(canonical) != journal.canonicalPayloadSha256) {
                throw ReleaseException("CANONICAL_HASH_MISMATCH", "canonicalPayloadSha256 校验失败")
            }
            if (config.requireSignature) {
                val key = config.trustedKeys[journal.signingKeyId]
                    ?: throw ReleaseException("UNTRUSTED_SIGNING_KEY", "signingKeyId 不在本机 TrustedKeys")
                if (!ReleaseSignatureVerifier.verify(key, canonical, signature)) {
                    throw ReleaseException("SIGNATURE_INVALID", "Ed25519 签名校验失败")
                }
            }
            journal = journal.copy(state = ReleaseState.PREPARED, files = files)
            store.save(journal)
            emit(
                journal,
                ReleaseAction.PREPARE,
                success = true,
                observedManifestRevision = observed.revision,
                resultManifestRevision = stageManifest.revision,
                message = "发布内容已完成签名校验并进入 stage",
            )
        } catch (failure: Throwable) {
            val releaseFailure = failure as? ReleaseException
                ?: ReleaseException("PREPARE_FAILED", failure.message ?: failure.javaClass.simpleName, failure)
            journal = journal.copy(
                state = ReleaseState.FAILED,
                lastErrorCode = releaseFailure.code,
                lastMessage = releaseFailure.message,
            )
            store.save(journal)
            emit(
                journal,
                ReleaseAction.PREPARE,
                success = false,
                observedManifestRevision = observed.revision,
                errorCode = releaseFailure.code,
                message = releaseFailure.message,
            )
        }
    }

    suspend fun commit(request: ReleaseRequest): ReleaseResult = lock(request.transactionId) {
        var journal = requireJournal(request)
        if (journal.state in setOf(ReleaseState.READINESS_PENDING, ReleaseState.READY)) {
            return@lock emit(journal, ReleaseAction.COMMIT, successFor(journal.state), message = "commit 已处理")
        }
        if (journal.state != ReleaseState.PREPARED) {
            throw ReleaseException("INVALID_TRANSACTION_STATE", "当前状态 ${journal.state} 不能 commit")
        }
        val config = configProvider()
        val readinessDeadline = requireNotNull(request.readinessDeadline)
        val observed = livePolicy(config).snapshotManifest()
        if (observed.revision != journal.expectedManifestRevision) {
            throw ReleaseException("MANIFEST_PRECONDITION_FAILED", "commit 前 live manifest 已变化")
        }
        if (System.currentTimeMillis() > readinessDeadline) {
            throw ReleaseException("READINESS_DEADLINE_EXPIRED", "readinessDeadline 已过期")
        }
        journal = journal.copy(
            commandId = request.commandId,
            state = ReleaseState.COMMITTING,
            readinessDeadline = readinessDeadline,
        )
        store.save(journal)
        journal = completeSwap(journal)
        journal = journal.copy(state = ReleaseState.READINESS_PENDING)
        store.save(journal)
        emit(
            journal,
            ReleaseAction.COMMIT,
            success = true,
            observedManifestRevision = observed.revision,
            resultManifestRevision = journal.targetManifestRevision,
            message = "磁盘交换完成，等待异步 readiness",
        )
    }

    suspend fun status(request: ReleaseRequest): ReleaseResult = lock(request.transactionId) {
        val journal = requireJournal(request)
        val resultRevision = runCatching { livePolicy(configProvider()).snapshotManifest().revision }.getOrNull()
        emit(
            journal.copy(commandId = request.commandId),
            ReleaseAction.STATUS,
            successFor(journal.state),
            resultManifestRevision = resultRevision,
            errorCode = journal.lastErrorCode,
            message = journal.lastMessage ?: "当前发布状态 ${journal.state.wireName()}",
        )
    }

    suspend fun rollback(request: ReleaseRequest): ReleaseResult = lock(request.transactionId) {
        var journal = requireJournal(request).copy(commandId = request.commandId)
        journal = rollbackInternal(journal, request.reason ?: "relay requested rollback")
        emit(
            journal,
            ReleaseAction.ROLLBACK,
            success = journal.state == ReleaseState.ROLLED_BACK,
            resultManifestRevision = runCatching { livePolicy(configProvider()).snapshotManifest().revision }.getOrNull(),
            errorCode = journal.lastErrorCode,
            message = journal.lastMessage,
        )
    }

    suspend fun completeReadiness(transactionId: String, readiness: suspend () -> ReadinessReport): ReleaseResult = lock(transactionId) {
        var journal = store.load(transactionId)
            ?: throw ReleaseException("TRANSACTION_NOT_FOUND", "发布事务不存在")
        if (journal.state == ReleaseState.READY) {
            return@lock emit(journal, ReleaseAction.STATUS, true, resultManifestRevision = journal.targetManifestRevision)
        }
        if (journal.state !in setOf(ReleaseState.READINESS_PENDING, ReleaseState.ACTIVATING)) {
            throw ReleaseException("INVALID_TRANSACTION_STATE", "当前状态不能执行 readiness")
        }
        journal = journal.copy(state = ReleaseState.ACTIVATING)
        store.save(journal)
        val deadline = journal.readinessDeadline ?: 0L
        val report = if (deadline > 0L && System.currentTimeMillis() > deadline) {
            ReadinessReport(false, "readinessDeadline 已过期")
        } else {
            runCatching { readiness() }.getOrElse {
                ReadinessReport(false, "重载异常: ${it.message ?: it.javaClass.simpleName}")
            }
        }
        val liveRevision = runCatching { livePolicy(configProvider()).snapshotManifest().revision }.getOrNull()
        if (report.success && liveRevision == journal.targetManifestRevision) {
            journal = journal.copy(state = ReleaseState.READY, lastErrorCode = null, lastMessage = report.message)
            store.save(journal)
            return@lock emit(
                journal,
                ReleaseAction.STATUS,
                true,
                resultManifestRevision = liveRevision,
                message = report.message,
            )
        }
        val failureMessage = if (report.success) "readiness 后 manifest 不匹配" else report.message
        journal = journal.copy(lastErrorCode = "READINESS_FAILED", lastMessage = failureMessage)
        store.save(journal)
        journal = rollbackInternal(journal, failureMessage)
        emit(
            journal,
            ReleaseAction.STATUS,
            success = false,
            resultManifestRevision = runCatching { livePolicy(configProvider()).snapshotManifest().revision }.getOrNull(),
            errorCode = journal.lastErrorCode,
            message = journal.lastMessage,
        )
    }

    suspend fun recover(onReadinessPending: suspend (String) -> Unit) {
        store.list().forEach { initial ->
            lock(initial.transactionId) {
                var journal = store.load(initial.transactionId) ?: return@lock
                try {
                    when (journal.state) {
                        ReleaseState.COMMITTING -> {
                            journal = completeSwap(journal)
                            journal = journal.copy(state = ReleaseState.READINESS_PENDING)
                            store.save(journal)
                        }
                        ReleaseState.ACTIVATING -> {
                            journal = journal.copy(state = ReleaseState.READINESS_PENDING)
                            store.save(journal)
                        }
                        ReleaseState.ROLLING_BACK -> rollbackInternal(journal, journal.lastMessage ?: "恢复未完成 rollback")
                        else -> Unit
                    }
                } catch (failure: Throwable) {
                    val latest = store.load(initial.transactionId) ?: journal
                    store.save(
                        latest.copy(
                            state = ReleaseState.RECOVERY_REQUIRED,
                            lastErrorCode = "RECOVERY_AMBIGUOUS",
                            lastMessage = failure.message ?: failure.javaClass.simpleName,
                        ),
                    )
                }
            }
            val current = store.load(initial.transactionId)
            if (current?.state == ReleaseState.READINESS_PENDING) onReadinessPending(initial.transactionId)
        }
    }

    fun failure(request: ReleaseRequest, failure: Throwable): ReleaseResult {
        val error = failure as? ReleaseException
        return ReleaseResult(
            action = request.action,
            transactionId = request.transactionId,
            releaseId = request.releaseId,
            commandId = request.commandId,
            success = false,
            pluginState = ReleaseState.FAILED,
            eventId = UUID.randomUUID().toString(),
            eventSeq = 1L,
            errorCode = error?.code ?: "RELEASE_REQUEST_FAILED",
            message = error?.message ?: failure.message ?: failure.javaClass.simpleName,
        )
    }

    private suspend fun buildFullStage(
        request: ReleaseRequest,
        files: List<ReleaseFile>,
        urlPolicy: ReleaseUrlPolicy,
        config: ReleaseConfig,
    ) {
        val stageRoot = store.stageDir(request.transactionId)
        val transferToken = requireNotNull(request.transferToken)
        allowlist.sortedDirectories().forEach { Files.createDirectories(stageRoot.resolve(it)) }
        val policy = stagePolicy(request.transactionId, config)
        files.sortedBy { it.ordinal }.forEach { file ->
            val target = policy.resolve(file.path)
            Files.createDirectories(target.parent)
            val temporary = target.parent.resolve(".${target.fileName}.${UUID.randomUUID()}.part")
            try {
                val downloaded = downloader.file(
                    urlPolicy.validateContentUrl(file.contentUrl),
                    transferToken,
                    config,
                    temporary,
                    file.size,
                )
                if (downloaded.sha256 != file.contentRevision) {
                    throw ReleaseException("CONTENT_SHA256_MISMATCH", "下载文件 SHA-256 不匹配: ${file.path}")
                }
                move(temporary, target)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }
        store.ensureNoLinks(stageRoot)
    }

    private fun validateOperationIdentity(
        operations: ReleaseOperations,
        request: ReleaseRequest,
        journal: ReleaseJournal,
    ) {
        if (operations.canonicalVersion != ReleaseCanonical.VERSION ||
            operations.canonicalPayloadSha256 != journal.canonicalPayloadSha256 ||
            operations.signingKeyId != journal.signingKeyId ||
            operations.signature != request.signature ||
            operations.releaseId != request.releaseId ||
            operations.expectedManifestRevision != journal.expectedManifestRevision ||
            operations.targetManifestRevision != journal.targetManifestRevision ||
            operations.fileCount != journal.fileCount ||
            operations.totalBytes != journal.totalBytes
        ) {
            throw ReleaseException("OPERATIONS_IDENTITY_MISMATCH", "operations metadata 与签名发布请求不一致")
        }
    }

    private fun validateOperations(
        files: List<ReleaseFile>,
        journal: ReleaseJournal,
        liveFiles: List<ManifestEntryV1>,
        urlPolicy: ReleaseUrlPolicy,
        config: ReleaseConfig,
    ) {
        if (files.size != journal.fileCount || files.map { it.ordinal }.sorted() != files.indices.toList()) {
            throw ReleaseException("OPERATIONS_FILE_COUNT_MISMATCH", "operations 文件数量或 ordinal 无效")
        }
        if (files.sumOf { it.size } != journal.totalBytes || journal.totalBytes > config.maxStagedBytes) {
            throw ReleaseException("OPERATIONS_TOTAL_BYTES_MISMATCH", "operations totalBytes 不一致")
        }
        val liveByPath = liveFiles.associateBy { it.path }
        val folded = mutableSetOf<String>()
        val componentCases = mutableMapOf<String, MutableMap<String, String>>()
        val allowedTopLevels = topLevels().toSet()
        val validationRoot = store.stageDir(journal.transactionId)
        val validationPolicy = EditorFilePolicy(validationRoot, maxFileBytes = config.maxStagedBytes, allowlist = allowlist)
        files.forEach { file ->
            if (file.size < 0L || file.size > config.maxStagedBytes) {
                throw ReleaseException("INVALID_FILE_SIZE", "文件大小无效: ${file.path}")
            }
            if (!SHA256.matches(file.contentRevision) || (file.baseRevision != null && !SHA256.matches(file.baseRevision))) {
                throw ReleaseException("INVALID_FILE_REVISION", "文件 revision 无效: ${file.path}")
            }
            if ('\\' in file.path || file.path.startsWith('/') || file.path.endsWith('/') || "//" in file.path) {
                throw ReleaseException("INVALID_FILE_PATH", "target 文件路径必须是规范的正斜杠相对路径: ${file.path}")
            }
            val components = file.path.split('/')
            if (components.first() !in allowedTopLevels) {
                throw ReleaseException("INVALID_FILE_PATH", "target 顶层路径大小写或 allowlist 无效: ${file.path}")
            }
            validationPolicy.resolve(file.path)
            val caseKey = file.path.lowercase(Locale.ROOT)
            if (!folded.add(caseKey)) throw ReleaseException("CASE_CONFLICT", "target 文件存在大小写冲突: ${file.path}")
            var parentKey = ""
            components.forEach { component ->
                val siblings = componentCases.getOrPut(parentKey) { mutableMapOf() }
                val foldedComponent = component.lowercase(Locale.ROOT)
                val previous = siblings.putIfAbsent(foldedComponent, component)
                if (previous != null && previous != component) {
                    throw ReleaseException("CASE_CONFLICT", "target 路径组件存在大小写冲突: $previous / $component")
                }
                parentKey = if (parentKey.isEmpty()) foldedComponent else "$parentKey/$foldedComponent"
            }
            val liveRevision = liveByPath[file.path]?.revision
            if (file.baseRevision != liveRevision) {
                throw ReleaseException("FILE_BASE_REVISION_MISMATCH", "文件 baseRevision 不匹配: ${file.path}")
            }
            urlPolicy.validateContentUrl(file.contentUrl)
        }
    }

    private fun decodeOperations(bytes: ByteArray): ReleaseOperations {
        return runCatching {
            json.decodeFromString(ReleaseOperations.serializer(), String(bytes, Charsets.UTF_8))
        }.getOrElse {
            if (it is ReleaseException) throw it
            throw ReleaseException("INVALID_OPERATIONS", "operations metadata 无法解析", it)
        }
    }

    private fun completeSwap(initial: ReleaseJournal): ReleaseJournal {
        var journal = initial
        val stageRoot = store.stageDir(journal.transactionId)
        val backupRoot = store.backupDir(journal.transactionId)
        store.ensureNoLinks(stageRoot)
        store.ensureNoLinks(backupRoot)
        topLevels().forEach { top ->
            val live = liveRoot.resolve(top)
            val stage = stageRoot.resolve(top)
            val backup = backupRoot.resolve(top)
            if (top !in journal.backupMoved) {
                if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.exists(live, LinkOption.NOFOLLOW_LINKS)) {
                        throw ReleaseException("RECOVERY_AMBIGUOUS", "live 与 backup 同时存在: $top")
                    }
                } else if (Files.exists(live, LinkOption.NOFOLLOW_LINKS)) {
                    rejectLink(live)
                    move(live, backup)
                    checkpoint("backup-moved", top)
                }
                journal = journal.copy(backupMoved = journal.backupMoved + top)
                store.save(journal)
            }
            if (top !in journal.stageMoved) {
                when {
                    Files.exists(stage, LinkOption.NOFOLLOW_LINKS) && Files.exists(live, LinkOption.NOFOLLOW_LINKS) ->
                        throw ReleaseException("RECOVERY_AMBIGUOUS", "live 与 stage 同时存在: $top")
                    Files.exists(stage, LinkOption.NOFOLLOW_LINKS) -> {
                        rejectLink(stage)
                        move(stage, live)
                        checkpoint("stage-moved", top)
                    }
                    else -> Unit
                }
                journal = journal.copy(stageMoved = journal.stageMoved + top)
                store.save(journal)
            }
        }
        return journal
    }

    private fun rollbackInternal(initial: ReleaseJournal, reason: String): ReleaseJournal {
        if (initial.backupMoved.isEmpty() && initial.stageMoved.isEmpty()) {
            store.deleteTree(store.stageDir(initial.transactionId))
            store.deleteTree(store.backupDir(initial.transactionId))
            val rolledBack = initial.copy(state = ReleaseState.ROLLED_BACK, lastMessage = reason)
            store.save(rolledBack)
            return rolledBack
        }
        var journal = initial.copy(state = ReleaseState.ROLLING_BACK, lastMessage = reason)
        store.save(journal)
        val backupRoot = store.backupDir(journal.transactionId)
        topLevels().asReversed().forEach { top ->
            val live = liveRoot.resolve(top)
            val backup = backupRoot.resolve(top)
            if (top !in journal.rollbackRemoved) {
                deleteTreeChecked(live)
                checkpoint("rollback-live-removed", top)
                journal = journal.copy(rollbackRemoved = journal.rollbackRemoved + top)
                store.save(journal)
            }
            if (top !in journal.rollbackRestored) {
                if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) {
                    rejectLink(backup)
                    if (Files.exists(live, LinkOption.NOFOLLOW_LINKS)) {
                        throw ReleaseException("RECOVERY_AMBIGUOUS", "rollback 恢复前 live 已存在: $top")
                    }
                    move(backup, live)
                    checkpoint("rollback-restored", top)
                }
                journal = journal.copy(rollbackRestored = journal.rollbackRestored + top)
                store.save(journal)
            }
        }
        val restored = livePolicy(configProvider()).snapshotManifest().revision
        journal = if (restored == journal.expectedManifestRevision) {
            journal.copy(state = ReleaseState.ROLLED_BACK, lastErrorCode = journal.lastErrorCode, lastMessage = reason)
        } else {
            journal.copy(
                state = ReleaseState.RECOVERY_REQUIRED,
                lastErrorCode = "ROLLBACK_MANIFEST_MISMATCH",
                lastMessage = "rollback 后 manifest 与 base 不一致",
            )
        }
        store.save(journal)
        return journal
    }

    private fun requireJournal(request: ReleaseRequest): ReleaseJournal {
        val journal = store.load(request.transactionId)
            ?: throw ReleaseException("TRANSACTION_NOT_FOUND", "发布事务不存在")
        validateTransactionIdentity(journal, request)
        return journal
    }

    private fun validateTransactionIdentity(journal: ReleaseJournal, request: ReleaseRequest) {
        if (journal.releaseId != request.releaseId) {
            throw ReleaseException("TRANSACTION_ID_CONFLICT", "transactionId 已绑定其他 releaseId")
        }
    }

    private fun emit(
        journal: ReleaseJournal,
        action: ReleaseAction,
        success: Boolean,
        observedManifestRevision: String? = null,
        resultManifestRevision: String? = null,
        errorCode: String? = null,
        message: String? = null,
    ): ReleaseResult {
        val eventId = UUID.randomUUID().toString()
        val updated = journal.copy(
            eventSeq = journal.eventSeq + 1,
            lastEventId = eventId,
            lastErrorCode = errorCode ?: journal.lastErrorCode,
            lastMessage = message ?: journal.lastMessage,
        )
        store.save(updated)
        return ReleaseResult(
            action = action,
            transactionId = updated.transactionId,
            releaseId = updated.releaseId,
            commandId = updated.commandId,
            success = success,
            pluginState = updated.state,
            eventId = eventId,
            eventSeq = updated.eventSeq,
            observedManifestRevision = observedManifestRevision,
            resultManifestRevision = resultManifestRevision,
            errorCode = errorCode,
            message = message,
        )
    }

    private fun livePolicy(config: ReleaseConfig): EditorFilePolicy =
        EditorFilePolicy(liveRoot, maxFileBytes = config.maxStagedBytes, allowlist = allowlist)

    private fun stagePolicy(transactionId: String, config: ReleaseConfig): EditorFilePolicy =
        EditorFilePolicy(store.stageDir(transactionId), maxFileBytes = config.maxStagedBytes, allowlist = allowlist)

    private fun topLevels(): List<String> = (allowlist.sortedDirectories() + allowlist.sortedRootFiles()).sorted()

    private fun successFor(state: ReleaseState): Boolean = state !in setOf(
        ReleaseState.FAILED,
        ReleaseState.RECOVERY_REQUIRED,
    )

    private suspend fun <T> lock(transactionId: String, block: suspend () -> T): T {
        return mutexes.computeIfAbsent(transactionId) { Mutex() }.withLock { block() }
    }

    private fun move(source: Path, target: Path) {
        Files.createDirectories(target.parent)
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }

    private fun rejectLink(path: Path) {
        if (Files.isSymbolicLink(path)) throw ReleaseException("SYMLINK_REJECTED", "发布交换禁止符号链接")
    }

    private fun deleteTreeChecked(path: Path) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
        rejectLink(path)
        Files.walk(path).use { stream ->
            stream.forEach { if (Files.isSymbolicLink(it)) throw ReleaseException("SYMLINK_REJECTED", "发布交换禁止符号链接") }
        }
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private val SHA256 = Regex("^[0-9a-f]{64}$")
}

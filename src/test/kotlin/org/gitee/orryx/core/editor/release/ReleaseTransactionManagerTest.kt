package org.gitee.orryx.core.editor.release

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.gitee.orryx.core.editor.handler.EditorFilePolicy
import org.gitee.orryx.core.editor.handler.EditorMutationGate
import org.gitee.orryx.core.editor.handler.EditorMutationOperation
import org.gitee.orryx.core.editor.handler.ManifestCanonicalHash
import org.gitee.orryx.core.editor.handler.ManifestEntryV1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

class ReleaseTransactionManagerTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `release request file count matches manifest contract boundary`() {
        fun request(fileCount: Int) = buildJsonObject {
            put("action", "prepare")
            put("transactionId", "11111111-1111-4111-8111-111111111111")
            put("releaseId", "22222222-2222-4222-8222-222222222222")
            put("commandId", "a".repeat(64))
            put("canonicalVersion", ReleaseCanonical.VERSION)
            put("canonicalPayloadSha256", "b".repeat(64))
            put("signingKeyId", "c".repeat(64))
            put("signature", "d".repeat(86))
            put("expectedManifestRevision", "e".repeat(64))
            put("targetManifestRevision", "f".repeat(64))
            put("fileCount", fileCount)
            put("totalBytes", 0L)
            put("operationsUrl", "https://release.example.test/releases/tx/operations.json")
            put("transferToken", "t".repeat(40))
            put("transferExpiresAt", 1L)
        }

        assertEquals(
            EditorFilePolicy.DEFAULT_MAX_MANIFEST_FILES,
            ReleaseRequest.parse(request(EditorFilePolicy.DEFAULT_MAX_MANIFEST_FILES)).fileCount,
        )
        val failure = assertThrows(ReleaseException::class.java) {
            ReleaseRequest.parse(request(EditorFilePolicy.DEFAULT_MAX_MANIFEST_FILES + 1))
        }
        assertEquals("INVALID_FILE_COUNT", failure.code)
    }

    @Test
    fun `prepare verifies complete stage manifest signature and content hashes`() = runTest {
        val fixture = fixture(root.resolve("valid"))
        val result = fixture.manager.prepare(fixture.prepareRequest)

        assertTrue(result.success)
        assertEquals(ReleaseState.PREPARED, result.pluginState)
        assertEquals(fixture.targetRevision, result.resultManifestRevision)

        val tampered = fixture(root.resolve("tampered"), tamperPath = "skills/old.yml")
        val failed = tampered.manager.prepare(tampered.prepareRequest)
        assertFalse(failed.success)
        assertEquals(ReleaseState.FAILED, failed.pluginState)
        assertEquals("CONTENT_SHA256_MISMATCH", failed.errorCode)
    }

    @Test
    fun `readiness is separate from commit and failure rolls back base`() = runTest {
        val fixture = fixture(root.resolve("readiness"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)

        val pending = fixture.manager.commit(fixture.commitRequest)
        assertEquals(ReleaseState.READINESS_PENDING, pending.pluginState)
        assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            fixture.gate.checkAllowed(EditorMutationOperation.FILE_WRITE)
        }
        assertEquals("new", String(Files.readAllBytes(fixture.live.resolve("skills/old.yml"))))
        assertTrue(Files.exists(fixture.live.resolve("keys.yml")))

        val reloadCount = AtomicInteger()
        val rolledBack = fixture.manager.completeReadiness(
            fixture.transactionId,
            compensatingReload = {
                reloadCount.incrementAndGet()
                ReadinessReport(true, "base reloaded")
            },
        ) {
            reloadCount.incrementAndGet()
            ReadinessReport(false, "synthetic reload failure")
        }
        assertFalse(rolledBack.success)
        assertEquals(ReleaseState.ROLLED_BACK, rolledBack.pluginState)
        assertEquals("base", String(Files.readAllBytes(fixture.live.resolve("skills/old.yml"))))
        assertFalse(Files.exists(fixture.live.resolve("keys.yml")))
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        assertEquals(2, reloadCount.get())
        fixture.gate.checkAllowed(EditorMutationOperation.RELOAD)
    }

    @Test
    fun `failed rollback compensation requires recovery and keeps mutation gate`() = runTest {
        val fixture = fixture(root.resolve("compensation-failure"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
        assertEquals(ReleaseState.READINESS_PENDING, fixture.manager.commit(fixture.commitRequest).pluginState)

        val result = fixture.manager.completeReadiness(
            fixture.transactionId,
            compensatingReload = { ReadinessReport(false, "base reload failed") },
        ) {
            ReadinessReport(false, "target reload failed")
        }

        assertEquals(ReleaseState.RECOVERY_REQUIRED, result.pluginState)
        assertEquals("ROLLBACK_RELOAD_FAILED", result.errorCode)
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            fixture.gate.checkAllowed(EditorMutationOperation.FILE_CREATE)
        }
    }

    @Test
    fun `explicit rollback reloads restored base before releasing mutation gate`() = runTest {
        val fixture = fixture(root.resolve("explicit-rollback"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
        assertEquals(ReleaseState.READINESS_PENDING, fixture.manager.commit(fixture.commitRequest).pluginState)
        val reloadCount = AtomicInteger()

        val result = fixture.manager.rollback(
            fixture.prepareRequest.copy(
                action = ReleaseAction.ROLLBACK,
                commandId = ReleaseCanonical.sha256("rollback:${fixture.transactionId}".toByteArray()),
                reason = "operator requested rollback",
            ),
        ) {
            reloadCount.incrementAndGet()
            ReadinessReport(true, "base reloaded")
        }

        assertTrue(result.success)
        assertEquals(ReleaseState.ROLLED_BACK, result.pluginState)
        assertEquals(1, reloadCount.get())
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        fixture.gate.checkAllowed(EditorMutationOperation.FILE_WRITE)
    }

    @Test
    fun `explicit rollback reload failure returns stable recovery code and keeps gate`() = runTest {
        val fixture = fixture(root.resolve("explicit-rollback-reload-failure"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
        assertEquals(ReleaseState.READINESS_PENDING, fixture.manager.commit(fixture.commitRequest).pluginState)

        val result = fixture.manager.rollback(
            fixture.prepareRequest.copy(
                action = ReleaseAction.ROLLBACK,
                commandId = ReleaseCanonical.sha256("rollback-failed:${fixture.transactionId}".toByteArray()),
            ),
        ) {
            ReadinessReport(false, "base reload failed")
        }

        assertFalse(result.success)
        assertEquals(ReleaseState.RECOVERY_REQUIRED, result.pluginState)
        assertEquals("ROLLBACK_RELOAD_FAILED", result.errorCode)
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            fixture.gate.checkAllowed(EditorMutationOperation.FILE_DELETE)
        }
    }

    @Test
    fun `startup rollback recovery compensates reload before releasing gate`() = runTest {
        val fixture = fixture(root.resolve("startup-rollback"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
        assertEquals(ReleaseState.READINESS_PENDING, fixture.manager.commit(fixture.commitRequest).pluginState)
        val store = ReleaseJournalStore(fixture.transactions)
        val pendingRollback = requireNotNull(store.load(fixture.transactionId)).copy(
            state = ReleaseState.ROLLING_BACK,
            lastMessage = "resume rollback",
        )
        store.save(pendingRollback)
        val reloadCount = AtomicInteger()
        val recoveredManager = ReleaseTransactionManager(
            liveRoot = fixture.live,
            transactionsRoot = fixture.transactions,
            configProvider = { fixture.config },
            downloader = fixture.downloader,
            mutationGate = fixture.gate,
        )

        recoveredManager.recover(
            compensatingReload = {
                reloadCount.incrementAndGet()
                ReadinessReport(true, "base reloaded")
            },
            onReadinessPending = { error("rollback recovery 不应进入 readiness") },
        )

        assertEquals(1, reloadCount.get())
        assertEquals(ReleaseState.ROLLED_BACK, store.load(fixture.transactionId)?.state)
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        fixture.gate.checkAllowed(EditorMutationOperation.RELOAD)
    }

    @Test
    fun `startup rollback reload failure keeps recovery gate and stable code`() = runTest {
        val fixture = fixture(root.resolve("startup-rollback-reload-failure"))
        assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
        assertEquals(ReleaseState.READINESS_PENDING, fixture.manager.commit(fixture.commitRequest).pluginState)
        val store = ReleaseJournalStore(fixture.transactions)
        store.save(
            requireNotNull(store.load(fixture.transactionId)).copy(
                state = ReleaseState.ROLLING_BACK,
                lastMessage = "resume rollback",
            ),
        )
        val recoveredManager = ReleaseTransactionManager(
            liveRoot = fixture.live,
            transactionsRoot = fixture.transactions,
            configProvider = { fixture.config },
            downloader = fixture.downloader,
            mutationGate = fixture.gate,
        )

        recoveredManager.recover(
            compensatingReload = { ReadinessReport(false, "base reload failed") },
            onReadinessPending = { error("rollback recovery 不应进入 readiness") },
        )

        val recovered = requireNotNull(store.load(fixture.transactionId))
        assertEquals(ReleaseState.RECOVERY_REQUIRED, recovered.state)
        assertEquals("ROLLBACK_RELOAD_FAILED", recovered.lastErrorCode)
        assertEquals(fixture.baseRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        assertThrows(EditorMutationGate.MutationBlockedException::class.java) {
            fixture.gate.checkAllowed(EditorMutationOperation.FILE_WRITE)
        }
    }

    @Test
    fun `every filesystem exchange checkpoint recovers to ready`() = runTest {
        val expectedCheckpointCount = 11
        for (crashAt in 1..expectedCheckpointCount) {
            val directory = root.resolve("crash-$crashAt")
            val counter = AtomicInteger()
            val fixture = fixture(directory) { _, _ ->
                if (counter.incrementAndGet() == crashAt) throw SimulatedCrash()
            }
            assertEquals(ReleaseState.PREPARED, fixture.manager.prepare(fixture.prepareRequest).pluginState)
            runCatching { fixture.manager.commit(fixture.commitRequest) }
                .onSuccess { error("checkpoint $crashAt 未触发模拟崩溃") }
                .onFailure { assertTrue(it is SimulatedCrash) }

            val recoveredManager = ReleaseTransactionManager(
                liveRoot = fixture.live,
                transactionsRoot = fixture.transactions,
                configProvider = { fixture.config },
                downloader = fixture.downloader,
                mutationGate = fixture.gate,
            )
            val pending = mutableListOf<String>()
            recoveredManager.recover(
                compensatingReload = { ReadinessReport(true, "base reloaded") },
                onReadinessPending = { pending += it },
            )
            assertEquals(listOf(fixture.transactionId), pending, "checkpoint=$crashAt")
            val ready = recoveredManager.completeReadiness(fixture.transactionId) {
                ReadinessReport(true, "ready")
            }
            assertEquals(ReleaseState.READY, ready.pluginState, "checkpoint=$crashAt")
            assertEquals(fixture.targetRevision, EditorFilePolicy(fixture.live).snapshotManifest().revision)
        }
    }

    private fun fixture(
        directory: Path,
        tamperPath: String? = null,
        checkpoint: (String, String) -> Unit = { _, _ -> },
    ): Fixture {
        val live = directory.resolve("live")
        val transactions = live.resolve(".editor/releases/transactions")
        Files.createDirectories(live.resolve("skills"))
        Files.write(live.resolve("skills/old.yml"), "base".toByteArray())
        val livePolicy = EditorFilePolicy(live)
        val base = livePolicy.snapshotManifest()

        val targetContents = linkedMapOf(
            "skills/old.yml" to "new".toByteArray(),
            "keys.yml" to "key: value".toByteArray(),
        )
        val files = targetContents.entries.mapIndexed { index, (path, bytes) ->
            ReleaseFile(
                ordinal = index,
                path = path,
                baseRevision = base.files.singleOrNull { it.path == path }?.revision,
                contentRevision = ReleaseCanonical.sha256(bytes),
                size = bytes.size.toLong(),
                contentUrl = "https://release.example.test/releases/tx/content/$index",
            )
        }
        val targetRevision = ManifestCanonicalHash.calculate(
            files.map { ManifestEntryV1(it.path, it.contentRevision, it.size) },
        )
        val privateKey = Ed25519PrivateKeyParameters(SecureRandom())
        val publicKey = privateKey.generatePublicKey().encoded
        val keyId = ReleaseCanonical.sha256(publicKey)
        val transactionId = "11111111-1111-4111-8111-${directory.fileName.toString().hashCode().toUInt().toString(16).padStart(12, '0').takeLast(12)}"
        val releaseId = "22222222-2222-4222-8222-222222222222"
        val commandId = ReleaseCanonical.sha256(directory.toString().toByteArray())
        val createdAt = 1_748_736_000_123L
        val payload = ReleaseCanonical.Payload(
            keyId = keyId,
            releaseId = releaseId,
            serverInstanceId = "33333333-3333-4333-8333-333333333333",
            stableServerId = "stable-server-test",
            draftId = "44444444-4444-4444-8444-444444444444",
            draftVersionId = "55555555-5555-4555-8555-555555555555",
            expectedBaseManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            createdAtEpochMillis = createdAt,
            files = files,
        )
        val canonical = ReleaseCanonical.encode(payload)
        val signer = Ed25519Signer().apply {
            init(true, privateKey)
            update(canonical, 0, canonical.size)
        }
        val signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.generateSignature())
        val config = ReleaseConfig(
            enabled = true,
            requireSignature = true,
            maxStagedBytes = 64L * 1024L * 1024L,
            connectTimeoutMillis = 1_000,
            readTimeoutMillis = 1_000,
            callTimeoutMillis = 2_000,
            acceptedClockSkewMillis = 30_000,
            allowLocalhostHttp = false,
            allowedHosts = setOf("release.example.test"),
            trustedKeys = mapOf(keyId to publicKey),
        )
        val contentsByUrl = files.associate { file ->
            val original = targetContents.getValue(file.path)
            file.contentUrl to if (file.path == tamperPath) original.copyOf().also { it[0]++ } else original
        }
        val operations = ReleaseOperations(
            canonicalVersion = ReleaseCanonical.VERSION,
            canonicalPayloadSha256 = ReleaseCanonical.sha256(canonical),
            signingKeyId = keyId,
            signature = signature,
            releaseId = releaseId,
            serverInstanceId = payload.serverInstanceId,
            stableServerId = payload.stableServerId,
            draftId = payload.draftId,
            draftVersionId = payload.draftVersionId,
            expectedManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            createdAt = createdAt,
            fileCount = files.size,
            totalBytes = files.sumOf { it.size },
            files = files,
        )
        val downloader = FakeDownloader(operations, contentsByUrl)
        val gate = EditorMutationGate()
        val manager = ReleaseTransactionManager(
            liveRoot = live,
            transactionsRoot = transactions,
            configProvider = { config },
            downloader = downloader,
            checkpoint = checkpoint,
            mutationGate = gate,
        )
        val prepare = ReleaseRequest(
            action = ReleaseAction.PREPARE,
            transactionId = transactionId,
            releaseId = releaseId,
            commandId = commandId,
            canonicalVersion = ReleaseCanonical.VERSION,
            canonicalPayloadSha256 = ReleaseCanonical.sha256(canonical),
            signingKeyId = keyId,
            signature = signature,
            expectedManifestRevision = base.revision,
            targetManifestRevision = targetRevision,
            fileCount = files.size,
            totalBytes = files.sumOf { it.size },
            operationsUrl = "https://release.example.test/releases/tx/operations.json",
            transferToken = "t".repeat(40),
            transferExpiresAt = System.currentTimeMillis() + 60_000,
        )
        val commit = ReleaseRequest(
            action = ReleaseAction.COMMIT,
            transactionId = transactionId,
            releaseId = releaseId,
            commandId = ReleaseCanonical.sha256("commit:$directory".toByteArray()),
            readinessDeadline = System.currentTimeMillis() + 60_000,
        )
        return Fixture(
            live,
            transactions,
            manager,
            downloader,
            gate,
            config,
            prepare,
            commit,
            transactionId,
            base.revision,
            targetRevision,
        )
    }

    private data class Fixture(
        val live: Path,
        val transactions: Path,
        val manager: ReleaseTransactionManager,
        val downloader: ReleaseDownloader,
        val gate: EditorMutationGate,
        val config: ReleaseConfig,
        val prepareRequest: ReleaseRequest,
        val commitRequest: ReleaseRequest,
        val transactionId: String,
        val baseRevision: String,
        val targetRevision: String,
    )

    private class FakeDownloader(
        operations: ReleaseOperations,
        private val contents: Map<String, ByteArray>,
    ) : ReleaseDownloader {
        private val metadata = Json.encodeToString(ReleaseOperations.serializer(), operations).toByteArray()

        override suspend fun metadata(uri: URI, token: String, config: ReleaseConfig): ByteArray = metadata

        override suspend fun file(
            uri: URI,
            token: String,
            config: ReleaseConfig,
            target: Path,
            expectedSize: Long,
        ): DownloadedFile {
            val bytes = requireNotNull(contents[uri.toString()])
            Files.createDirectories(target.parent)
            Files.write(target, bytes)
            return DownloadedFile(bytes.size.toLong(), ReleaseCanonical.sha256(bytes))
        }
    }

    private class SimulatedCrash : RuntimeException()
}

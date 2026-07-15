package org.gitee.orryx.core.editor.release

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.gitee.orryx.core.editor.EditorProtocol
import org.gitee.orryx.core.editor.handler.EditorFilePolicy
import java.net.URI
import java.util.UUID

internal enum class ReleaseAction(val wireName: String) {
    PREPARE("prepare"),
    COMMIT("commit"),
    STATUS("status"),
    ROLLBACK("rollback");

    companion object {
        fun parse(value: String?): ReleaseAction? = entries.firstOrNull { it.wireName == value }
    }
}

@Serializable
internal enum class ReleaseState {
    PREPARING,
    PREPARED,
    COMMITTING,
    ACTIVATING,
    READINESS_PENDING,
    READY,
    ROLLING_BACK,
    ROLLED_BACK,
    FAILED,
    RECOVERY_REQUIRED,
}

internal fun ReleaseState.wireName(): String = when (this) {
    ReleaseState.ACTIVATING -> "READINESS_PENDING"
    else -> name
}

@Serializable
internal data class ReleaseFile(
    val ordinal: Int,
    val path: String,
    val baseRevision: String? = null,
    val contentRevision: String,
    val size: Long,
    val contentUrl: String,
)

@Serializable
internal data class ReleaseOperations(
    val canonicalVersion: String,
    val canonicalPayloadSha256: String,
    val signingKeyId: String,
    val signature: String,
    val releaseId: String,
    val serverInstanceId: String,
    val stableServerId: String,
    val draftId: String,
    val draftVersionId: String,
    val expectedManifestRevision: String,
    val targetManifestRevision: String,
    val createdAt: Long,
    val fileCount: Int,
    val totalBytes: Long,
    val files: List<ReleaseFile>,
)

internal data class ReleaseRequest(
    val action: ReleaseAction,
    val transactionId: String,
    val releaseId: String,
    val commandId: String,
    val canonicalVersion: String? = null,
    val canonicalPayloadSha256: String? = null,
    val signingKeyId: String? = null,
    val signature: String? = null,
    val expectedManifestRevision: String? = null,
    val targetManifestRevision: String? = null,
    val fileCount: Int? = null,
    val totalBytes: Long? = null,
    val operationsUrl: String? = null,
    val transferToken: String? = null,
    val transferExpiresAt: Long? = null,
    val readinessDeadline: Long? = null,
    val reason: String? = null,
) {
    companion object {
        private val UUID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        private val SIGNATURE_PATTERN = Regex("^[A-Za-z0-9_-]{86}$")

        fun parse(data: JsonObject?): ReleaseRequest {
            val source = data ?: throw ReleaseException("INVALID_REQUEST", "release.request data 缺失")
            val action = ReleaseAction.parse(source.string("action"))
                ?: throw ReleaseException("INVALID_ACTION", "release action 无效")
            val transactionId = source.requiredString("transactionId")
            val releaseId = source.requiredString("releaseId")
            val commandId = source.requiredString("commandId")
            if (!UUID_PATTERN.matches(transactionId) || runCatching { UUID.fromString(transactionId) }.isFailure) {
                throw ReleaseException("INVALID_TRANSACTION_ID", "transactionId 必须是小写 UUID")
            }
            if (!UUID_PATTERN.matches(releaseId) || runCatching { UUID.fromString(releaseId) }.isFailure) {
                throw ReleaseException("INVALID_RELEASE_ID", "releaseId 必须是小写 UUID")
            }
            if (!EditorProtocol.isSha256Revision(commandId)) {
                throw ReleaseException("INVALID_COMMAND_ID", "commandId 必须是 64 位小写 SHA-256")
            }
            val request = ReleaseRequest(
                action = action,
                transactionId = transactionId,
                releaseId = releaseId,
                commandId = commandId,
                canonicalVersion = source.string("canonicalVersion"),
                canonicalPayloadSha256 = source.string("canonicalPayloadSha256"),
                signingKeyId = source.string("signingKeyId"),
                signature = source.string("signature"),
                expectedManifestRevision = source.string("expectedManifestRevision"),
                targetManifestRevision = source.string("targetManifestRevision"),
                fileCount = (source["fileCount"] as? JsonPrimitive)?.intOrNull,
                totalBytes = (source["totalBytes"] as? JsonPrimitive)?.longOrNull,
                operationsUrl = source.string("operationsUrl"),
                transferToken = source.string("transferToken"),
                transferExpiresAt = (source["transferExpiresAt"] as? JsonPrimitive)?.longOrNull,
                readinessDeadline = (source["readinessDeadline"] as? JsonPrimitive)?.longOrNull,
                reason = source.string("reason"),
            )
            request.validateActionFields()
            return request
        }

        private fun JsonObject.string(name: String): String? =
            (get(name) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        private fun JsonObject.requiredString(name: String): String =
            string(name) ?: throw ReleaseException("INVALID_REQUEST", "$name 缺失")

        private fun ReleaseRequest.validateActionFields() {
            if (action == ReleaseAction.PREPARE) {
                if (canonicalVersion != ReleaseCanonical.VERSION) {
                    throw ReleaseException("UNSUPPORTED_CANONICAL_VERSION", "canonicalVersion 必须是 ${ReleaseCanonical.VERSION}")
                }
                listOf(
                    "canonicalPayloadSha256" to canonicalPayloadSha256,
                    "signingKeyId" to signingKeyId,
                    "expectedManifestRevision" to expectedManifestRevision,
                    "targetManifestRevision" to targetManifestRevision,
                ).forEach { (name, value) ->
                    if (!EditorProtocol.isSha256Revision(value)) {
                        throw ReleaseException("INVALID_REQUEST", "$name 必须是 64 位小写 SHA-256")
                    }
                }
                if (signature == null || !SIGNATURE_PATTERN.matches(signature)) {
                    throw ReleaseException("INVALID_SIGNATURE", "signature 必须是无填充 Base64URL Ed25519 签名")
                }
                if (fileCount == null || fileCount < 0 || fileCount > EditorFilePolicy.DEFAULT_MAX_MANIFEST_FILES) {
                    throw ReleaseException("INVALID_FILE_COUNT", "fileCount 超出允许范围")
                }
                if (totalBytes == null || totalBytes < 0L) {
                    throw ReleaseException("INVALID_TOTAL_BYTES", "totalBytes 无效")
                }
                val url = operationsUrl ?: throw ReleaseException("INVALID_OPERATIONS_URL", "operationsUrl 缺失")
                runCatching { URI(url) }.getOrElse {
                    throw ReleaseException("INVALID_OPERATIONS_URL", "operationsUrl 不是有效 URI")
                }
                if (transferToken == null || transferToken.length !in 40..256) {
                    throw ReleaseException("INVALID_TRANSFER_TOKEN", "transferToken 长度无效")
                }
                if (transferExpiresAt == null || transferExpiresAt <= 0L) {
                    throw ReleaseException("INVALID_TRANSFER_EXPIRY", "transferExpiresAt 无效")
                }
            }
            if (action == ReleaseAction.COMMIT && (readinessDeadline == null || readinessDeadline <= 0L)) {
                throw ReleaseException("INVALID_READINESS_DEADLINE", "commit.readinessDeadline 缺失或无效")
            }
            if (reason != null && reason.length > 160) {
                throw ReleaseException("INVALID_REASON", "rollback.reason 过长")
            }
        }
    }
}

@Serializable
internal data class ReleaseJournal(
    val transactionId: String,
    val releaseId: String,
    val commandId: String,
    val prepareCommandId: String,
    val state: ReleaseState,
    val canonicalPayloadSha256: String,
    val signingKeyId: String,
    val expectedManifestRevision: String,
    val targetManifestRevision: String,
    val fileCount: Int,
    val totalBytes: Long,
    val operationsUrl: String,
    val transferExpiresAt: Long,
    val files: List<ReleaseFile> = emptyList(),
    val backupMoved: Set<String> = emptySet(),
    val stageMoved: Set<String> = emptySet(),
    val rollbackRemoved: Set<String> = emptySet(),
    val rollbackRestored: Set<String> = emptySet(),
    val eventSeq: Long = 0L,
    val lastEventId: String? = null,
    val lastErrorCode: String? = null,
    val lastMessage: String? = null,
    val readinessDeadline: Long? = null,
)

internal data class ReleaseResult(
    val action: ReleaseAction,
    val transactionId: String,
    val releaseId: String,
    val commandId: String,
    val success: Boolean,
    val pluginState: ReleaseState,
    val eventId: String,
    val eventSeq: Long,
    val observedManifestRevision: String? = null,
    val resultManifestRevision: String? = null,
    val errorCode: String? = null,
    val message: String? = null,
)

internal class ReleaseException(
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

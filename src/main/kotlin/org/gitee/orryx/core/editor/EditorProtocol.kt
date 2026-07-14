package org.gitee.orryx.core.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Orryx Editor relay 协议常量、DTO 与消息方向约束。 */
object EditorProtocol {

    const val PROTOCOL_V1 = "v1"
    const val PROTOCOL_V2 = "v2"
    const val DEFAULT_PROTOCOL = PROTOCOL_V1

    const val SERVER_REGISTER = "server.register"
    const val SERVER_REGISTER_RESULT = "server.register.result"
    const val MANIFEST_GET = "manifest.get"
    const val MANIFEST_SNAPSHOT = "manifest.snapshot"
    const val ERROR = "error"

    val V2_CAPABILITIES: List<String> = listOf(
        "protocol.allowlist",
        "revision.sha256",
        "mutation.preconditions",
    )

    private val SHA256_REVISION = Regex("^[0-9a-f]{64}$")

    private val centerToServerTypes = setOf(
        SERVER_REGISTER_RESULT,
        ERROR,
        "token.register.result",
        "token.revoke.result",
        "file.list",
        "file.read",
        "file.write",
        "file.create",
        "file.delete",
        "file.rename",
        "reload",
        "log.subscribe",
        "log.unsubscribe",
    )

    private val serverToCenterTypes = setOf(
        SERVER_REGISTER,
        "token.register",
        "token.revoke",
        "file.tree",
        "file.content",
        "file.written",
        "file.changed",
        "reload.result",
        "log.subscribe.result",
        "log.unsubscribe.result",
        "log.entry",
        "server.info",
        ERROR,
    )

    fun supportedProtocols(v2Enabled: Boolean): List<String> {
        return if (v2Enabled) listOf(PROTOCOL_V2, PROTOCOL_V1) else listOf(PROTOCOL_V1)
    }

    fun preferredProtocol(v2Enabled: Boolean): String {
        return if (v2Enabled) PROTOCOL_V2 else PROTOCOL_V1
    }

    fun registrationData(request: ServerRegisterRequest): JsonObject {
        return buildJsonObject {
            put("license", request.license)
            put("serverName", request.serverName)
            put("serverId", request.serverId)
            put("pluginVersion", request.pluginVersion)
            put("protocolVersions", JsonArray(request.protocolVersions.map(::JsonPrimitive)))
            put("preferredProtocol", request.preferredProtocol)
            put("capabilities", JsonArray(request.capabilities.map(::JsonPrimitive)))
            put("connectionNonce", request.connectionNonce)
        }
    }

    /**
     * relay V1 不返回协商字段时明确回落到 V1；V2 字段存在时则保存会话元数据。
     */
    fun parseRegisterResult(data: JsonObject?): ServerRegisterResult {
        val success = (data?.get("success") as? JsonPrimitive)?.booleanOrNull ?: false
        val negotiatedProtocolValue = (data?.get("negotiatedProtocol") as? JsonPrimitive)?.contentOrNull
        val negotiatedProtocol = normalizeProtocol(negotiatedProtocolValue) ?: negotiatedProtocolValue ?: PROTOCOL_V1
        val relayCapabilities = (data?.get("relayCapabilities") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .orEmpty()
        return ServerRegisterResult(
            success = success,
            message = (data?.get("message") as? JsonPrimitive)?.contentOrNull.orEmpty(),
            negotiatedProtocol = negotiatedProtocol,
            serverId = (data?.get("serverId") as? JsonPrimitive)?.contentOrNull,
            sessionEpoch = (data?.get("sessionEpoch") as? JsonPrimitive)?.longOrNull,
            workspaceId = (data?.get("workspaceId") as? JsonPrimitive)?.contentOrNull,
            relayCapabilities = relayCapabilities,
            connectionNonce = (data?.get("connectionNonce") as? JsonPrimitive)?.contentOrNull,
        )
    }

    fun validateNegotiatedProtocol(result: ServerRegisterResult, offeredProtocols: Collection<String>): Boolean {
        return result.negotiatedProtocol in offeredProtocols
    }

    fun validateRegisterResult(result: ServerRegisterResult, request: ServerRegisterRequest): ServerRegisterValidation {
        val protocolAccepted = validateNegotiatedProtocol(result, request.protocolVersions)
        val serverIdAccepted = result.serverId == null || result.serverId == request.serverId
        val nonceAccepted = result.connectionNonce == null || result.connectionNonce == request.connectionNonce
        val sessionMetadataAccepted =
            (result.sessionEpoch == null || result.sessionEpoch > 0L) &&
                (result.workspaceId == null || isSha256Revision(result.workspaceId))
        val v2ContractAccepted = result.negotiatedProtocol != PROTOCOL_V2 ||
            (
                result.serverId == request.serverId &&
                    result.sessionEpoch != null && result.sessionEpoch > 0L &&
                    isSha256Revision(result.workspaceId) &&
                    result.connectionNonce == request.connectionNonce &&
                    "revision.sha256" in result.relayCapabilities
                )
        return ServerRegisterValidation(
            protocolAccepted = protocolAccepted,
            serverIdAccepted = serverIdAccepted,
            nonceAccepted = nonceAccepted,
            sessionMetadataAccepted = sessionMetadataAccepted,
            v2ContractAccepted = v2ContractAccepted,
        )
    }

    fun isSha256Revision(value: String?): Boolean = value != null && SHA256_REVISION.matches(value)

    fun normalizeProtocol(value: String?): String? {
        return when (value?.trim()?.lowercase()) {
            "1", PROTOCOL_V1 -> PROTOCOL_V1
            "2", PROTOCOL_V2 -> PROTOCOL_V2
            else -> null
        }
    }

    fun inboundDisposition(type: String): InboundDisposition {
        return when {
            type in centerToServerTypes -> InboundDisposition.ACCEPT
            type in serverToCenterTypes -> InboundDisposition.WRONG_DIRECTION
            else -> InboundDisposition.UNKNOWN
        }
    }

    fun isServerToCenter(type: String): Boolean = type in serverToCenterTypes

    internal fun allowedInboundTypes(): Set<String> = centerToServerTypes

    internal fun allowedOutboundTypes(): Set<String> = serverToCenterTypes
}

@Serializable
data class ServerRegisterRequest(
    val license: String,
    val serverName: String,
    val serverId: String,
    val pluginVersion: String,
    val protocolVersions: List<String>,
    val preferredProtocol: String,
    val capabilities: List<String>,
    val connectionNonce: String,
)

@Serializable
data class ServerRegisterResult(
    val success: Boolean,
    val message: String,
    val negotiatedProtocol: String,
    val serverId: String? = null,
    val sessionEpoch: Long? = null,
    val workspaceId: String? = null,
    val relayCapabilities: List<String> = emptyList(),
    val connectionNonce: String? = null,
)

data class ServerRegisterValidation(
    val protocolAccepted: Boolean,
    val serverIdAccepted: Boolean,
    val nonceAccepted: Boolean,
    val sessionMetadataAccepted: Boolean,
    val v2ContractAccepted: Boolean,
) {
    val accepted: Boolean
        get() = protocolAccepted && serverIdAccepted && nonceAccepted && sessionMetadataAccepted && v2ContractAccepted
}

enum class InboundDisposition {
    ACCEPT,
    WRONG_DIRECTION,
    UNKNOWN,
}

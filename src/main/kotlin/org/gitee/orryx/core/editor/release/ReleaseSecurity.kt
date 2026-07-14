package org.gitee.orryx.core.editor.release

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.gitee.orryx.api.Orryx
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.util.Base64
import java.util.Locale

internal data class ReleaseConfig(
    val enabled: Boolean,
    val requireSignature: Boolean,
    val maxStagedBytes: Long,
    val connectTimeoutMillis: Long,
    val readTimeoutMillis: Long,
    val callTimeoutMillis: Long,
    val acceptedClockSkewMillis: Long,
    val allowLocalhostHttp: Boolean,
    val allowedHosts: Set<String>,
    val trustedKeys: Map<String, ByteArray>,
) {
    companion object {
        fun load(): ReleaseConfig {
            val section = Orryx.config.getConfigurationSection("Editor.Release.TrustedKeys")
            val keys = section?.getKeys(false).orEmpty().associateWith { keyId ->
                val encoded = section?.getString(keyId)?.trim().orEmpty()
                decodeTrustedKey(keyId, encoded)
            }
            return ReleaseConfig(
                enabled = Orryx.config.getBoolean("Editor.Release.Enable", false),
                requireSignature = Orryx.config.getBoolean("Editor.Release.RequireSignature", true),
                maxStagedBytes = Orryx.config.getLong("Editor.Release.MaxStagedBytes", 64L * 1024L * 1024L),
                connectTimeoutMillis = Orryx.config.getLong("Editor.Release.HttpConnectTimeoutMillis", 10_000L),
                readTimeoutMillis = Orryx.config.getLong("Editor.Release.HttpReadTimeoutMillis", 30_000L),
                callTimeoutMillis = Orryx.config.getLong("Editor.Release.HttpCallTimeoutMillis", 60_000L),
                acceptedClockSkewMillis = Orryx.config.getLong("Editor.Release.AcceptedClockSkewMillis", 30_000L),
                allowLocalhostHttp = Orryx.config.getBoolean("Editor.Release.AllowLocalhostHttp", false),
                allowedHosts = Orryx.config.getStringList("Editor.Release.AllowedHosts")
                    .mapTo(linkedSetOf()) { it.trim().lowercase(Locale.ROOT) }
                    .filterTo(linkedSetOf()) { it.isNotEmpty() },
                trustedKeys = keys,
            ).validated()
        }

        internal fun decodeTrustedKey(keyId: String, encoded: String): ByteArray {
            if (!SHA256.matches(keyId)) throw ReleaseException("INVALID_TRUSTED_KEY", "TrustedKeys keyId 必须是 SHA-256")
            val bytes = runCatching { Base64.getDecoder().decode(encoded) }.getOrElse {
                throw ReleaseException("INVALID_TRUSTED_KEY", "TrustedKeys.$keyId 不是有效 Base64", it)
            }
            if (bytes.size != Ed25519PublicKeyParameters.KEY_SIZE) {
                throw ReleaseException("INVALID_TRUSTED_KEY", "TrustedKeys.$keyId 必须是 raw 32 字节 Ed25519 公钥")
            }
            if (ReleaseCanonical.sha256(bytes) != keyId) {
                throw ReleaseException("INVALID_TRUSTED_KEY", "TrustedKeys.$keyId 与公钥 SHA-256 指纹不匹配")
            }
            return bytes
        }

        private val SHA256 = Regex("^[0-9a-f]{64}$")
    }

    private fun validated(): ReleaseConfig {
        if (maxStagedBytes <= 0L || maxStagedBytes > 64L * 1024L * 1024L) {
            throw ReleaseException("INVALID_RELEASE_CONFIG", "MaxStagedBytes 必须位于 1..67108864")
        }
        if (connectTimeoutMillis <= 0L || readTimeoutMillis <= 0L || callTimeoutMillis <= 0L) {
            throw ReleaseException("INVALID_RELEASE_CONFIG", "HTTP timeout 必须大于 0")
        }
        if (acceptedClockSkewMillis < 0L || acceptedClockSkewMillis > 300_000L) {
            throw ReleaseException("INVALID_RELEASE_CONFIG", "AcceptedClockSkewMillis 超出允许范围")
        }
        return this
    }
}

internal object ReleaseSignatureVerifier {

    fun verify(publicKey: ByteArray, canonical: ByteArray, signature: String): Boolean {
        if (publicKey.size != Ed25519PublicKeyParameters.KEY_SIZE) return false
        val signatureBytes = runCatching { Base64.getUrlDecoder().decode(signature) }.getOrNull() ?: return false
        if (signatureBytes.size != 64) return false
        val verifier = Ed25519Signer()
        verifier.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        verifier.update(canonical, 0, canonical.size)
        return verifier.verifySignature(signatureBytes)
    }
}

internal class ReleaseUrlPolicy(
    operationsUrl: String,
    private val config: ReleaseConfig,
) {
    val operationsUri: URI = parseAndValidate(operationsUrl, isOperations = true)
    private val basePath: String = operationsUri.path.substringBeforeLast('/', missingDelimiterValue = "/").let {
        if (it.endsWith('/')) it else "$it/"
    }

    fun validateContentUrl(value: String): URI {
        val uri = parseAndValidate(value, isOperations = false)
        if (!sameOrigin(operationsUri, uri)) {
            throw ReleaseException("CONTENT_URL_NOT_ALLOWED", "contentUrl 必须与 operationsUrl 同源")
        }
        if (!uri.path.startsWith(basePath)) {
            throw ReleaseException("CONTENT_URL_NOT_ALLOWED", "contentUrl 必须位于 operationsUrl 的发布目录下")
        }
        return uri
    }

    private fun parseAndValidate(value: String, isOperations: Boolean): URI {
        val uri = runCatching { URI(value) }.getOrElse {
            throw ReleaseException("INVALID_OPERATIONS_URL", "发布下载 URL 无效", it)
        }
        if (!uri.isAbsolute || uri.rawUserInfo != null || uri.rawFragment != null || uri.host.isNullOrBlank()) {
            throw ReleaseException("INVALID_OPERATIONS_URL", "发布下载 URL 必须是无凭据、无 fragment 的绝对 URL")
        }
        val rawPath = uri.rawPath.orEmpty().lowercase(Locale.ROOT)
        if (rawPath.contains("%2e") || rawPath.contains("%2f") || rawPath.contains("%5c")) {
            throw ReleaseException("INVALID_OPERATIONS_URL", "发布下载 URL 路径禁止编码的点或分隔符")
        }
        val scheme = uri.scheme.lowercase(Locale.ROOT)
        val host = uri.host.lowercase(Locale.ROOT)
        val localhost = isLocalhostName(host)
        if (scheme != "https" && !(scheme == "http" && localhost && config.allowLocalhostHttp)) {
            throw ReleaseException("INSECURE_OPERATIONS_URL", "发布下载仅允许 HTTPS；localhost HTTP 需显式开启")
        }
        if (host !in config.allowedHosts) {
            val addresses = runCatching { InetAddress.getAllByName(host).toList() }.getOrElse {
                throw ReleaseException("OPERATIONS_HOST_UNRESOLVED", "发布下载主机无法解析", it)
            }
            if (addresses.isEmpty() || addresses.any(::isPrivateAddress)) {
                if (!(localhost && config.allowLocalhostHttp)) {
                    throw ReleaseException("OPERATIONS_HOST_NOT_ALLOWED", "发布下载主机解析到私网或本地地址")
                }
            }
        }
        if (isOperations && uri.path.isNullOrEmpty()) {
            throw ReleaseException("INVALID_OPERATIONS_URL", "operationsUrl 缺少路径")
        }
        return uri.normalize()
    }

    private fun sameOrigin(first: URI, second: URI): Boolean {
        return first.scheme.equals(second.scheme, ignoreCase = true) &&
            first.host.equals(second.host, ignoreCase = true) &&
            effectivePort(first) == effectivePort(second)
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    private fun isLocalhostName(host: String): Boolean = host == "localhost" || host.endsWith(".localhost")

    private fun isPrivateAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) {
            return true
        }
        return when (address) {
            is Inet4Address -> {
                val bytes = address.address.map { it.toInt() and 0xff }
                bytes[0] == 0 || bytes[0] == 10 || bytes[0] == 127 ||
                    (bytes[0] == 169 && bytes[1] == 254) ||
                    (bytes[0] == 172 && bytes[1] in 16..31) ||
                    (bytes[0] == 192 && bytes[1] == 168) ||
                    (bytes[0] == 100 && bytes[1] in 64..127) ||
                    bytes[0] >= 224
            }
            is Inet6Address -> address.isIPv4CompatibleAddress || address.hostAddress.lowercase(Locale.ROOT).startsWith("fc") ||
                address.hostAddress.lowercase(Locale.ROOT).startsWith("fd")
            else -> true
        }
    }
}

package org.gitee.orryx.core.editor.release

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** 与服务端冻结一致的 orryx-release-v1 canonical 编码。 */
internal object ReleaseCanonical {

    const val VERSION = "orryx-release-v1"
    private val MAGIC = "ORRYX-RELEASE\u0000".toByteArray(StandardCharsets.US_ASCII)
    private const val BINARY_VERSION = 1

    data class Payload(
        val keyId: String,
        val releaseId: String,
        val serverInstanceId: String,
        val stableServerId: String,
        val draftId: String,
        val draftVersionId: String,
        val expectedBaseManifestRevision: String,
        val targetManifestRevision: String,
        val createdAtEpochMillis: Long,
        val files: List<ReleaseFile>,
    )

    fun encode(payload: Payload): ByteArray {
        val files = payload.files.sortedBy { it.path }
        require(files.map { it.path }.distinct().size == files.size) { "target 文件路径不能重复" }
        require(SHA256.matches(payload.expectedBaseManifestRevision)) { "expected manifest 无效" }
        require(SHA256.matches(payload.targetManifestRevision)) { "target manifest 无效" }
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.write(MAGIC)
            data.writeByte(BINARY_VERSION)
            data.writeUtf8(payload.keyId)
            data.writeUtf8(payload.releaseId)
            data.writeUtf8(payload.serverInstanceId)
            data.writeUtf8(payload.stableServerId)
            data.writeUtf8(payload.draftId)
            data.writeUtf8(payload.draftVersionId)
            data.writeUtf8(payload.expectedBaseManifestRevision)
            data.writeUtf8(payload.targetManifestRevision)
            data.writeLong(payload.createdAtEpochMillis)
            data.writeInt(files.size)
            files.forEach { file ->
                require(file.size >= 0L) { "target 文件大小不能为负数" }
                data.writeUtf8(file.path)
                if (file.baseRevision == null) {
                    data.writeByte(0)
                } else {
                    data.writeByte(1)
                    data.writeSha256(file.baseRevision)
                }
                data.writeSha256(file.contentRevision)
                data.writeLong(file.size)
            }
        }
        return output.toByteArray()
    }

    fun sha256(payload: Payload): String = sha256(encode(payload))

    fun sha256(bytes: ByteArray): String = toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun sha256DigestToHex(bytes: ByteArray): String = toHex(bytes)

    private fun DataOutputStream.writeUtf8(value: String) {
        val encoded = value.toByteArray(StandardCharsets.UTF_8)
        writeInt(encoded.size)
        write(encoded)
    }

    private fun DataOutputStream.writeSha256(value: String) {
        require(SHA256.matches(value)) { "无效 SHA-256: $value" }
        write(hexToBytes(value))
    }

    private fun hexToBytes(value: String): ByteArray = ByteArray(value.length / 2) { index ->
        ((value[index * 2].digitToInt(16) shl 4) or value[index * 2 + 1].digitToInt(16)).toByte()
    }

    private fun toHex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { value ->
            val unsigned = value.toInt() and 0xff
            append(HEX[unsigned ushr 4])
            append(HEX[unsigned and 0x0f])
        }
    }

    private val SHA256 = Regex("^[0-9a-f]{64}$")
    private const val HEX = "0123456789abcdef"
}

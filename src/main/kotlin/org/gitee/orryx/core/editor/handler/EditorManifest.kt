package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Locale

/** Editor 可远程访问内容的显式描述；内部 `.editor` 目录永远不在此描述中。 */
data class EditorFileAllowlistDescriptor(
    val directories: Set<String>,
    val rootFiles: Set<String>,
) {

    private val normalizedDirectories = directories.mapTo(linkedSetOf()) { it.lowercase(Locale.ROOT) }
    private val normalizedRootFiles = rootFiles.mapTo(linkedSetOf()) { it.lowercase(Locale.ROOT) }

    fun allowsTopLevel(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT)
        return normalized in normalizedDirectories || normalized in normalizedRootFiles
    }

    fun isRootFile(name: String): Boolean = name.lowercase(Locale.ROOT) in normalizedRootFiles

    fun sortedDirectories(): List<String> = normalizedDirectories.sorted()

    fun sortedRootFiles(): List<String> = normalizedRootFiles.sorted()

    companion object {
        val ORRYX_CONFIG = EditorFileAllowlistDescriptor(
            directories = setOf(
                "skills", "jobs", "stations", "controllers", "experiences", "status",
                "ui", "lang", "placeholders",
            ),
            rootFiles = setOf(
                "keys.yml", "bloom.yml", "buffs.yml", "npc.yml", "selectors.yml", "state.yml",
            ),
        )
    }
}

@Serializable
data class ManifestSnapshotV1(
    val manifestId: String,
    val revision: String,
    val files: List<ManifestEntryV1>,
    val createdAt: Long? = null,
) {
    val rootHash: String
        get() = revision
}

@Serializable
data class ManifestEntryV1(
    val path: String,
    val revision: String,
    val size: Long,
)

/** 与平台无关的 manifest canonical root hash。 */
object ManifestCanonicalHash {

    fun calculate(entries: List<ManifestEntryV1>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("orryx-editor-manifest-v1\n".toByteArray(StandardCharsets.UTF_8))
        entries.sortedBy { it.path }.forEach { entry ->
            updateField(digest, entry.path)
            updateField(digest, entry.size.toString())
            updateField(digest, entry.revision)
            digest.update('\n'.code.toByte())
        }
        return EditorSha256.toHex(digest.digest())
    }

    private fun updateField(digest: MessageDigest, value: String) {
        digest.update(value.toByteArray(StandardCharsets.UTF_8))
        digest.update(0.toByte())
    }
}

internal data class StreamedFile(
    val bytes: ByteArray?,
    val size: Long,
    val sha256: String,
)

/** SHA-256 始终按流计算，读取内容时也不会先无界分配文件大小。 */
internal object EditorSha256 {

    fun read(path: Path, maxBytes: Long, includeBytes: Boolean): StreamedFile {
        val declaredSize = Files.size(path)
        if (declaredSize > maxBytes) {
            throw EditorFilePolicy.PolicyException("文件超过大小限制: $declaredSize > $maxBytes bytes")
        }
        Files.newInputStream(path).use { input ->
            return read(input, maxBytes, includeBytes)
        }
    }

    fun digest(bytes: ByteArray): String {
        return toHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    }

    private fun read(input: InputStream, maxBytes: Long, includeBytes: Boolean): StreamedFile {
        val digest = MessageDigest.getInstance("SHA-256")
        val output = if (includeBytes) ByteArrayOutputStream() else null
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > maxBytes) {
                throw EditorFilePolicy.PolicyException("文件超过大小限制: $total > $maxBytes bytes")
            }
            digest.update(buffer, 0, count)
            output?.write(buffer, 0, count)
        }
        return StreamedFile(output?.toByteArray(), total, toHex(digest.digest()))
    }

    fun toHex(bytes: ByteArray): String {
        return buildString(bytes.size * 2) {
            bytes.forEach { byte ->
                val value = byte.toInt() and 0xff
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0f])
            }
        }
    }

    private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    private const val HEX_DIGITS = "0123456789abcdef"
}

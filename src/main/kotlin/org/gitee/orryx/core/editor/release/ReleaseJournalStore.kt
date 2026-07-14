package org.gitee.orryx.core.editor.release

import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator
import java.util.UUID

internal class ReleaseJournalStore(private val transactionsRoot: Path) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        prettyPrint = false
    }

    init {
        Files.createDirectories(transactionsRoot)
        if (Files.isSymbolicLink(transactionsRoot)) {
            throw ReleaseException("INVALID_TRANSACTION_ROOT", "发布事务目录不能是符号链接")
        }
    }

    fun transactionDir(transactionId: String): Path = safeTransactionDir(transactionId)

    fun journalPath(transactionId: String): Path = safeTransactionDir(transactionId).resolve(JOURNAL_FILE)

    fun stageDir(transactionId: String): Path = safeTransactionDir(transactionId).resolve("stage")

    fun backupDir(transactionId: String): Path = safeTransactionDir(transactionId).resolve("backup")

    fun createLayout(transactionId: String) {
        val directory = safeTransactionDir(transactionId)
        Files.createDirectories(directory)
        Files.createDirectories(directory.resolve("stage"))
        Files.createDirectories(directory.resolve("backup"))
        ensureNoLinks(directory)
    }

    fun load(transactionId: String): ReleaseJournal? {
        val path = journalPath(transactionId)
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return null
        if (Files.isSymbolicLink(path)) throw ReleaseException("INVALID_JOURNAL", "journal 不能是符号链接")
        if (Files.size(path) > MAX_JOURNAL_BYTES) throw ReleaseException("INVALID_JOURNAL", "journal.json 超过大小限制")
        return runCatching {
            json.decodeFromString<ReleaseJournal>(String(Files.readAllBytes(path), StandardCharsets.UTF_8))
        }.getOrElse { throw ReleaseException("INVALID_JOURNAL", "journal.json 无法解析", it) }
    }

    fun list(): List<ReleaseJournal> {
        if (!Files.isDirectory(transactionsRoot, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        return Files.newDirectoryStream(transactionsRoot).use { stream ->
            stream.asSequence()
                .filter { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) }
                .mapNotNull { directory -> runCatching { load(directory.fileName.toString()) }.getOrNull() }
                .toList()
        }
    }

    fun save(journal: ReleaseJournal) {
        createLayout(journal.transactionId)
        val target = journalPath(journal.transactionId)
        val bytes = json.encodeToString(ReleaseJournal.serializer(), journal).toByteArray(StandardCharsets.UTF_8)
        val temporary = target.parent.resolve(".$JOURNAL_FILE.${UUID.randomUUID()}.tmp")
        try {
            FileChannel.open(
                temporary,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            ).use { channel ->
                var buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (unsupported: AtomicMoveNotSupportedException) {
                throw ReleaseException("ATOMIC_JOURNAL_UNSUPPORTED", "当前文件系统不支持 journal 原子替换", unsupported)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun deleteTree(path: Path) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
        ensureInside(path)
        ensureNoLinks(path)
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    fun ensureNoLinks(path: Path) {
        ensureInside(path)
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(path).use { stream ->
            stream.forEach {
                if (Files.isSymbolicLink(it)) {
                    throw ReleaseException("SYMLINK_REJECTED", "发布事务目录中禁止符号链接")
                }
            }
        }
    }

    private fun safeTransactionDir(transactionId: String): Path {
        if (!TRANSACTION_ID.matches(transactionId)) {
            throw ReleaseException("INVALID_TRANSACTION_ID", "transactionId 无效")
        }
        val path = transactionsRoot.resolve(transactionId).normalize()
        ensureInside(path)
        return path
    }

    private fun ensureInside(path: Path) {
        if (!path.toAbsolutePath().normalize().startsWith(transactionsRoot.toAbsolutePath().normalize())) {
            throw ReleaseException("TRANSACTION_PATH_ESCAPE", "发布事务路径越界")
        }
    }

    companion object {
        private const val JOURNAL_FILE = "journal.json"
        private const val MAX_JOURNAL_BYTES = 4L * 1024L * 1024L
        private val TRANSACTION_ID = Regex("^[0-9a-f-]{36}$")
    }
}

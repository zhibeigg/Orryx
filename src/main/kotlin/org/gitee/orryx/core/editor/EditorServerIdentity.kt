package org.gitee.orryx.core.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Orryx Editor 的本地稳定身份。
 *
 * 身份文件仅保存在插件目录的 `.editor/identity.json`，该目录不属于 Editor 远程文件 allowlist。
 * 此类型不包含密钥或 license。
 */
@Serializable
data class EditorServerIdentity(
    val schemaVersion: Int = SCHEMA_VERSION,
    val serverId: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * 可直接测试的同步文件服务。运行时必须从 [org.gitee.orryx.api.OrryxAPI.ioScope] 调用。
 */
class EditorServerIdentityStore(
    pluginDirectory: Path,
    private val idFactory: () -> UUID = UUID::randomUUID,
) {

    val identityPath: Path = pluginDirectory.toAbsolutePath().normalize()
        .resolve(IDENTITY_DIRECTORY)
        .resolve(IDENTITY_FILE)

    private val pathLock = PATH_LOCKS.computeIfAbsent(identityPath) { Any() }
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        prettyPrint = true
    }

    fun loadOrCreate(): EditorServerIdentity = synchronized(pathLock) {
        val directory = identityPath.parent ?: throw IdentityException("Editor 身份路径缺少父目录")
        prepareDirectory(directory)
        if (Files.exists(identityPath, LinkOption.NOFOLLOW_LINKS)) {
            return@synchronized loadExisting()
        }

        val identity = EditorServerIdentity(serverId = idFactory().toString())
        writeNewAtomically(identity)
        loadExisting()
    }

    private fun prepareDirectory(directory: Path) {
        val pluginDirectory = directory.parent ?: throw IdentityException("插件目录无效")
        Files.createDirectories(pluginDirectory)
        if (Files.isSymbolicLink(pluginDirectory)) {
            throw IdentityException("插件目录不能是符号链接")
        }
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                throw IdentityException("Editor 身份目录必须是普通目录")
            }
        } else {
            Files.createDirectory(directory)
        }
        applyOwnerOnlyDirectoryPermissions(directory)
    }

    private fun loadExisting(): EditorServerIdentity {
        if (Files.isSymbolicLink(identityPath) || !Files.isRegularFile(identityPath, LinkOption.NOFOLLOW_LINKS)) {
            throw IdentityException("Editor 身份文件必须是普通文件")
        }
        val size = Files.size(identityPath)
        if (size <= 0L || size > MAX_IDENTITY_BYTES) {
            throw IdentityException("Editor 身份文件大小无效")
        }
        val text = String(Files.readAllBytes(identityPath), StandardCharsets.UTF_8)
        val identity = try {
            json.decodeFromString(EditorServerIdentity.serializer(), text)
        } catch (e: Exception) {
            throw IdentityException("Editor 身份文件格式无效", e)
        }
        if (identity.schemaVersion != EditorServerIdentity.SCHEMA_VERSION) {
            throw IdentityException("不支持的 Editor 身份版本: ${identity.schemaVersion}")
        }
        val parsed = try {
            UUID.fromString(identity.serverId)
        } catch (e: IllegalArgumentException) {
            throw IdentityException("Editor serverId 不是有效 UUID", e)
        }
        if (parsed.toString() != identity.serverId) {
            throw IdentityException("Editor serverId 必须使用规范 UUID 格式")
        }
        applyOwnerOnlyFilePermissions(identityPath)
        return identity
    }

    private fun writeNewAtomically(identity: EditorServerIdentity) {
        val directory = identityPath.parent ?: throw IdentityException("Editor 身份路径缺少父目录")
        val temporary = Files.createTempFile(directory, ".identity-", ".tmp")
        try {
            val bytes = json.encodeToString(EditorServerIdentity.serializer(), identity)
                .toByteArray(StandardCharsets.UTF_8)
            Files.newOutputStream(
                temporary,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                output.write(bytes)
                output.flush()
            }
            applyOwnerOnlyFilePermissions(temporary)
            try {
                Files.move(temporary, identityPath, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, identityPath)
            } catch (_: FileAlreadyExistsException) {
                // 另一个进程或 Store 实例已先完成创建，随后统一加载并校验已有身份。
            }
            applyOwnerOnlyFilePermissions(identityPath)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun applyOwnerOnlyDirectoryPermissions(path: Path) {
        applyPosixPermissions(
            path,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
    }

    private fun applyOwnerOnlyFilePermissions(path: Path) {
        applyPosixPermissions(path, setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE))
    }

    private fun applyPosixPermissions(path: Path, permissions: Set<PosixFilePermission>) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path, permissions)
            }
        } catch (_: UnsupportedOperationException) {
        } catch (_: IOException) {
            // 权限收紧是尽力而为；身份内容不包含私钥，文件完整性仍由路径与格式校验保证。
        }
    }

    class IdentityException(message: String, cause: Throwable? = null) : IOException(message, cause)

    companion object {
        const val IDENTITY_DIRECTORY = ".editor"
        const val IDENTITY_FILE = "identity.json"
        private const val MAX_IDENTITY_BYTES = 4L * 1024L
        private val PATH_LOCKS = ConcurrentHashMap<Path, Any>()
    }
}

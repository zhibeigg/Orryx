package org.gitee.orryx.core.editor.handler

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator
import java.util.Locale

/**
 * Editor 文件访问策略。
 *
 * 所有路径均按相对路径组件解析，不允许访问根目录外部、根目录本身或任何符号链接。
 * 同时为单文件大小、目录深度与树节点数量提供基础配额，避免远程操作无限消耗资源。
 */
class EditorFilePolicy(
    root: Path,
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    private val maxTreeEntries: Int = DEFAULT_MAX_TREE_ENTRIES,
    private val maxTreeDepth: Int = DEFAULT_MAX_TREE_DEPTH,
) {

    val root: Path = root.toAbsolutePath().normalize()

    init {
        require(maxFileBytes > 0) { "maxFileBytes 必须大于 0" }
        require(maxTreeEntries > 0) { "maxTreeEntries 必须大于 0" }
        require(maxTreeDepth > 0) { "maxTreeDepth 必须大于 0" }
        Files.createDirectories(this.root)
        if (Files.isSymbolicLink(this.root)) {
            throw PolicyException("Editor 根目录不能是符号链接")
        }
    }

    data class TreeEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val children: List<TreeEntry> = emptyList(),
    )

    class PolicyException(message: String) : IOException(message)

    fun listTree(path: String?): List<TreeEntry> {
        val relativePath = path.orEmpty()
        val directory = resolve(relativePath, allowRoot = true)
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目录不存在: $relativePath")
        }
        val budget = TreeBudget()
        return buildTree(directory, relativePath.replace('\\', '/').trim('/'), 0, budget)
    }

    fun readText(path: String): String {
        val file = resolve(path)
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $path")
        }
        val size = Files.size(file)
        if (size > maxFileBytes) {
            throw PolicyException("文件超过大小限制: $size > $maxFileBytes bytes")
        }
        val bytes = Files.readAllBytes(file)
        if (bytes.size.toLong() > maxFileBytes) {
            throw PolicyException("文件超过大小限制: ${bytes.size} > $maxFileBytes bytes")
        }
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun writeTextAtomic(path: String, content: String) {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size.toLong() > maxFileBytes) {
            throw PolicyException("写入内容超过大小限制: ${bytes.size} > $maxFileBytes bytes")
        }

        val file = resolve(path)
        val fileExists = Files.exists(file, LinkOption.NOFOLLOW_LINKS)
        if (fileExists && !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目标不是普通文件: $path")
        }
        if (!fileExists) ensureEntryCapacity()
        val parent = file.parent ?: throw PolicyException("目标路径缺少父目录")
        createDirectories(parent)
        ensureNoSymbolicLinks(file)

        val temporary = Files.createTempFile(parent, ".orryx-editor-", ".tmp")
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            try {
                Files.move(
                    temporary,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun create(path: String, directory: Boolean) {
        ensureEntryCapacity()
        val target = resolve(path)
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件已存在: $path")
        }
        val parent = target.parent ?: throw PolicyException("目标路径缺少父目录")
        createDirectories(parent)
        ensureNoSymbolicLinks(target)
        if (directory) {
            Files.createDirectory(target)
        } else {
            Files.createFile(target)
        }
    }

    fun delete(path: String): Boolean {
        val target = resolve(path)
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $path")
        }
        ensureTreeHasNoSymbolicLinks(target)
        Files.walk(target).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }
        return !Files.exists(target, LinkOption.NOFOLLOW_LINKS)
    }

    fun rename(oldPath: String, newPath: String): Boolean {
        val source = resolve(oldPath)
        val target = resolve(newPath)
        if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $oldPath")
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目标已存在: $newPath")
        }
        ensureTreeHasNoSymbolicLinks(source)
        val parent = target.parent ?: throw PolicyException("目标路径缺少父目录")
        createDirectories(parent)
        ensureNoSymbolicLinks(target)
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
        return Files.exists(target, LinkOption.NOFOLLOW_LINKS)
    }

    internal fun resolve(path: String, allowRoot: Boolean = false): Path {
        if (path.length > MAX_PATH_LENGTH) {
            throw PolicyException("路径过长")
        }
        if (path.indexOf('\u0000') >= 0) {
            throw PolicyException("路径包含非法字符")
        }
        if (path.startsWith('/') || path.startsWith('\\') || DRIVE_PREFIX.matches(path)) {
            throw PolicyException("只允许使用相对路径")
        }

        val normalizedInput = path.replace('\\', '/')
        if (normalizedInput.isEmpty()) {
            if (!allowRoot) throw PolicyException("禁止修改 Editor 根目录")
            return root
        }

        val components = normalizedInput.split('/')
        if (components.any { !isSafeComponent(it) }) {
            throw PolicyException("路径包含非法组件")
        }
        if (!isAllowedTopLevel(components.first())) {
            throw PolicyException("该路径不在 Editor 允许的配置范围内")
        }

        val resolved = components.fold(root) { current, component -> current.resolve(component) }.normalize()
        if (!resolved.startsWith(root)) {
            throw PolicyException("路径越界")
        }
        if (!allowRoot && resolved == root) {
            throw PolicyException("禁止修改 Editor 根目录")
        }
        ensureNoSymbolicLinks(resolved)
        return resolved
    }

    private fun isSafeComponent(component: String): Boolean {
        if (component.isEmpty() || component == "." || component == ".." || component.length > MAX_COMPONENT_LENGTH) {
            return false
        }
        val lower = component.lowercase(Locale.ROOT)
        if (component.startsWith('.') || component.endsWith('~') || SENSITIVE_SUFFIXES.any(lower::endsWith)) {
            return false
        }
        if (component.any { it.code < 32 || it == ':' }) {
            return false
        }
        if (component.last() == ' ' || component.last() == '.') {
            return false
        }
        val windowsBaseName = component.substringBefore('.').uppercase(Locale.ROOT)
        return windowsBaseName !in WINDOWS_RESERVED_NAMES
    }

    private fun isAllowedTopLevel(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT)
        return normalized in ALLOWED_DIRECTORIES || normalized in ALLOWED_ROOT_FILES
    }

    private fun ensureNoSymbolicLinks(target: Path) {
        var current = root
        val relative = root.relativize(target)
        relative.forEach { component ->
            current = current.resolve(component)
            if (Files.isSymbolicLink(current)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(current)}")
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return
            }
        }
    }

    private fun ensureTreeHasNoSymbolicLinks(target: Path) {
        ensureNoSymbolicLinks(target)
        if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(target).use { stream ->
            stream.forEach {
                if (Files.isSymbolicLink(it)) {
                    throw PolicyException("禁止访问符号链接: ${displayPath(it)}")
                }
            }
        }
    }

    private fun createDirectories(directory: Path) {
        if (!directory.startsWith(root)) {
            throw PolicyException("路径越界")
        }
        var current = root
        root.relativize(directory).forEach { component ->
            current = current.resolve(component)
            if (Files.isSymbolicLink(current)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(current)}")
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                Files.createDirectory(current)
            } else if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw PolicyException("父路径不是目录: ${displayPath(current)}")
            }
        }
    }

    private fun buildTree(directory: Path, relativePath: String, depth: Int, budget: TreeBudget): List<TreeEntry> {
        if (depth > maxTreeDepth) {
            throw PolicyException("目录深度超过限制: $maxTreeDepth")
        }
        val children = Files.newDirectoryStream(directory).use { stream ->
            stream.iterator().asSequence()
                .filter { child -> relativePath.isNotEmpty() || isAllowedTopLevel(child.fileName.toString()) }
                .toList()
        }.sortedWith(compareByDescending<Path> { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) }.thenBy { it.fileName.toString() })

        return children.map { child ->
            if (Files.isSymbolicLink(child)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(child)}")
            }
            budget.consume()
            val name = child.fileName.toString()
            val childRelativePath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            val isDirectory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)
            TreeEntry(
                name = name,
                path = childRelativePath,
                isDirectory = isDirectory,
                children = if (isDirectory) buildTree(child, childRelativePath, depth + 1, budget) else emptyList(),
            )
        }
    }

    private fun ensureEntryCapacity() {
        val budget = TreeBudget()
        Files.walk(root).use { stream ->
            stream.forEach {
                if (it != root) budget.consume()
                if (Files.isSymbolicLink(it)) {
                    throw PolicyException("禁止访问符号链接: ${displayPath(it)}")
                }
            }
        }
        if (budget.entries >= maxTreeEntries) {
            throw PolicyException("文件数量已达到限制: $maxTreeEntries")
        }
    }

    private fun displayPath(path: Path): String = root.relativize(path).joinToString("/")

    private inner class TreeBudget {
        var entries: Int = 0
            private set

        fun consume() {
            entries++
            if (entries > maxTreeEntries) {
                throw PolicyException("文件数量超过限制: $maxTreeEntries")
            }
        }
    }

    companion object {
        internal const val DEFAULT_MAX_FILE_BYTES = 2L * 1024L * 1024L
        internal const val DEFAULT_MAX_TREE_ENTRIES = 10_000
        internal const val DEFAULT_MAX_TREE_DEPTH = 32
        private const val MAX_PATH_LENGTH = 1024
        private const val MAX_COMPONENT_LENGTH = 255
        private val DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
        private val ALLOWED_DIRECTORIES = setOf(
            "skills", "jobs", "stations", "controllers", "experiences", "status",
            "ui", "lang", "placeholders",
        )
        private val ALLOWED_ROOT_FILES = setOf(
            "keys.yml", "bloom.yml", "buffs.yml", "npc.yml", "selectors.yml", "state.yml",
        )
        private val SENSITIVE_SUFFIXES = setOf(
            ".db", ".sqlite", ".sqlite3", ".log", ".key", ".pem", ".p12", ".pfx", ".jks",
            ".keystore", ".env", ".bak", ".backup", ".tmp", ".temp",
        )
        private val WINDOWS_RESERVED_NAMES = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        )
    }
}

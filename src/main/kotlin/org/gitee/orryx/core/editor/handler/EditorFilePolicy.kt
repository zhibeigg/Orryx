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

enum class ExpectedPathState {
    PRESENT,
    ABSENT,
}

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
    val allowlist: EditorFileAllowlistDescriptor = EditorFileAllowlistDescriptor.ORRYX_CONFIG,
) {

    val root: Path = root.toAbsolutePath().normalize()
    private val realRoot: Path

    init {
        require(maxFileBytes > 0) { "maxFileBytes 必须大于 0" }
        require(maxTreeEntries > 0) { "maxTreeEntries 必须大于 0" }
        require(maxTreeDepth > 0) { "maxTreeDepth 必须大于 0" }
        Files.createDirectories(this.root)
        if (Files.isSymbolicLink(this.root)) {
            throw PolicyException("Editor 根目录不能是符号链接")
        }
        realRoot = this.root.toRealPath()
    }

    data class TreeEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val children: List<TreeEntry> = emptyList(),
    )

    data class FileContent(
        val content: String,
        val revision: String,
    )

    open class PolicyException(message: String) : IOException(message)

    class RevisionConflictException(message: String) : PolicyException(message)

    class PreconditionFailedException(message: String) : PolicyException(message)

    class CaseConflictException(message: String) : PolicyException(message)

    fun listTree(path: String?): List<TreeEntry> {
        val relativePath = path.orEmpty()
        val directory = resolve(relativePath, allowRoot = true)
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目录不存在: $relativePath")
        }
        val directoryDepth = if (directory == root) 0 else root.relativize(directory).nameCount
        ensureDepth(directoryDepth)
        val budget = TreeBudget()
        return buildTree(directory, relativePath.replace('\\', '/').trim('/'), directoryDepth, budget)
    }

    fun readText(path: String): String = readTextWithRevision(path).content

    fun readTextWithRevision(path: String): FileContent {
        val file = resolve(path)
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $path")
        }
        val streamed = EditorSha256.read(file, maxFileBytes, includeBytes = true)
        val bytes = streamed.bytes ?: throw PolicyException("读取文件内容失败: $path")
        return FileContent(String(bytes, StandardCharsets.UTF_8), streamed.sha256)
    }

    fun snapshotManifest(): ManifestSnapshotV1 {
        val budget = TreeBudget()
        val entries = mutableListOf<ManifestEntryV1>()
        appendManifestEntries(root, "", 0, budget, entries)
        val sortedEntries = entries.sortedBy { it.path }
        return ManifestSnapshotV1(
            manifestId = DEFAULT_MANIFEST_ID,
            revision = ManifestCanonicalHash.calculate(sortedEntries),
            files = sortedEntries,
        )
    }

    /**
     * 原子写入文件并返回新 revision。未传 [expectedRevision] 时保持旧协议的覆盖写入语义。
     */
    fun writeTextAtomic(path: String, content: String, expectedRevision: String? = null): String {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size.toLong() > maxFileBytes) {
            throw PolicyException("写入内容超过大小限制: ${bytes.size} > $maxFileBytes bytes")
        }

        val file = resolveMutationPath(path)
        ensureNoCaseAlias(file)
        val fileExists = Files.exists(file, LinkOption.NOFOLLOW_LINKS)
        if (fileExists && !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目标不是普通文件: $path")
        }
        if (expectedRevision != null) {
            if (!SHA256_REVISION.matches(expectedRevision)) {
                throw PolicyException("expectedRevision 必须是 64 位小写 SHA-256")
            }
            if (!fileExists) {
                throw RevisionConflictException("文件版本冲突: $path 已不存在")
            }
            val currentRevision = EditorSha256.read(file, maxFileBytes, includeBytes = false).sha256
            if (currentRevision != expectedRevision) {
                throw RevisionConflictException("文件版本冲突: $path 已被修改")
            }
        }

        val parent = file.parent ?: throw PolicyException("目标路径缺少父目录")
        if (!fileExists) {
            ensureCapacityFor(countMissingDirectories(parent) + 1)
        }
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
        return EditorSha256.digest(bytes)
    }

    fun create(path: String, directory: Boolean, expectedState: ExpectedPathState? = null) {
        val target = resolveMutationPath(path)
        ensureNoCaseAlias(target)
        expectedState?.let { validateExpectedState(target, path, it) }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件已存在: $path")
        }
        val parent = target.parent ?: throw PolicyException("目标路径缺少父目录")
        ensureCapacityFor(countMissingDirectories(parent) + 1)
        createDirectories(parent)
        ensureNoSymbolicLinks(target)
        if (directory) {
            Files.createDirectory(target)
        } else {
            Files.createFile(target)
        }
    }

    fun delete(path: String, expectedState: ExpectedPathState? = null): Boolean {
        val target = resolve(path)
        expectedState?.let { validateExpectedState(target, path, it) }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $path")
        }
        ensureTreeHasNoSymbolicLinks(target)
        Files.walk(target).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        }
        return !Files.exists(target, LinkOption.NOFOLLOW_LINKS)
    }

    fun rename(
        oldPath: String,
        newPath: String,
        expectedSourceState: ExpectedPathState? = null,
        expectedTargetState: ExpectedPathState? = null,
    ): Boolean {
        val source = resolve(oldPath)
        val target = resolveMutationPath(newPath)
        ensureNoCaseAlias(target, ignoredPath = source)
        expectedSourceState?.let { validateExpectedState(source, oldPath, it) }
        expectedTargetState?.let { validateExpectedState(target, newPath, it) }
        if (!Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("文件不存在: $oldPath")
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw PolicyException("目标已存在: $newPath")
        }
        if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) && target.startsWith(source)) {
            throw PolicyException("不能将目录移动到自身内部")
        }

        ensureTreeHasNoSymbolicLinks(source)
        ensureRenameDepth(source, target)
        val parent = target.parent ?: throw PolicyException("目标路径缺少父目录")
        ensureCapacityFor(countMissingDirectories(parent))
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

    private fun resolveMutationPath(path: String): Path {
        val resolved = resolve(path)
        ensureDepth(root.relativize(resolved).nameCount)
        return resolved
    }

    private fun ensureDepth(depth: Int) {
        if (depth > maxTreeDepth) {
            throw PolicyException("目录深度超过限制: $maxTreeDepth")
        }
    }

    private fun ensureRenameDepth(source: Path, target: Path) {
        val targetDepth = root.relativize(target).nameCount
        ensureDepth(targetDepth)
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(source).use { stream ->
            stream.forEach { child ->
                val descendantDepth = if (child == source) 0 else source.relativize(child).nameCount
                ensureDepth(targetDepth + descendantDepth)
            }
        }
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

    private fun isAllowedTopLevel(name: String): Boolean = allowlist.allowsTopLevel(name)

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
            ensureRealPathInsideRoot(current)
        }
    }

    private fun ensureRealPathInsideRoot(path: Path) {
        val realPath = path.toRealPath()
        if (!realPath.startsWith(realRoot)) {
            throw PolicyException("禁止访问指向 Editor 根目录外部的路径: ${displayPath(path)}")
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
                ensureRealPathInsideRoot(it)
            }
        }
    }

    private fun countMissingDirectories(directory: Path): Int {
        if (!directory.startsWith(root)) {
            throw PolicyException("路径越界")
        }
        var current = root
        var missing = 0
        root.relativize(directory).forEach { component ->
            current = current.resolve(component)
            if (Files.isSymbolicLink(current)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(current)}")
            }
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                missing++
            } else if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw PolicyException("父路径不是目录: ${displayPath(current)}")
            }
        }
        return missing
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

    private fun buildTree(
        directory: Path,
        relativePath: String,
        directoryDepth: Int,
        budget: TreeBudget,
    ): List<TreeEntry> {
        val children = listChildrenChecked(directory, filterAllowlist = relativePath.isEmpty())
            .sortedWith(
                compareByDescending<Path> { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) }
                    .thenBy { it.fileName.toString() },
            )

        return children.map { child ->
            if (Files.isSymbolicLink(child)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(child)}")
            }
            ensureRealPathInsideRoot(child)
            ensureDepth(directoryDepth + 1)
            budget.consume()
            val name = child.fileName.toString()
            val childRelativePath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            val isDirectory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)
            TreeEntry(
                name = name,
                path = childRelativePath,
                isDirectory = isDirectory,
                children = if (isDirectory) {
                    buildTree(child, childRelativePath, directoryDepth + 1, budget)
                } else {
                    emptyList()
                },
            )
        }
    }

    private fun appendManifestEntries(
        directory: Path,
        relativePath: String,
        directoryDepth: Int,
        budget: TreeBudget,
        entries: MutableList<ManifestEntryV1>,
    ) {
        val children = listChildrenChecked(directory, filterAllowlist = relativePath.isEmpty())
            .sortedBy { it.fileName.toString() }
        children.forEach { child ->
            if (Files.isSymbolicLink(child)) {
                throw PolicyException("禁止访问符号链接: ${displayPath(child)}")
            }
            ensureRealPathInsideRoot(child)
            ensureDepth(directoryDepth + 1)
            budget.consume()
            val name = child.fileName.toString()
            val childRelativePath = if (relativePath.isEmpty()) name else "$relativePath/$name"
            when {
                Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS) -> {
                    appendManifestEntries(child, childRelativePath, directoryDepth + 1, budget, entries)
                }
                Files.isRegularFile(child, LinkOption.NOFOLLOW_LINKS) -> {
                    val streamed = EditorSha256.read(child, maxFileBytes, includeBytes = false)
                    entries += ManifestEntryV1(
                        path = childRelativePath,
                        revision = streamed.sha256,
                        size = streamed.size,
                    )
                }
                else -> throw PolicyException("只允许普通文件和目录: $childRelativePath")
            }
        }
    }

    private fun listChildrenChecked(directory: Path, filterAllowlist: Boolean): List<Path> {
        val children = Files.newDirectoryStream(directory).use { stream ->
            stream.iterator().asSequence()
                .filter { child -> !filterAllowlist || isAllowedTopLevel(child.fileName.toString()) }
                .toList()
        }
        val namesByFoldedCase = mutableMapOf<String, String>()
        children.forEach { child ->
            val name = child.fileName.toString()
            val previous = namesByFoldedCase.put(name.lowercase(Locale.ROOT), name)
            if (previous != null && previous != name) {
                throw CaseConflictException("检测到仅大小写不同的路径冲突: $previous / $name")
            }
        }
        return children
    }

    private fun ensureNoCaseAlias(target: Path, ignoredPath: Path? = null) {
        var parent = root
        root.relativize(target).forEach { component ->
            if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) return
            val expectedName = component.toString()
            val conflict = Files.newDirectoryStream(parent).use { stream ->
                stream.firstOrNull { child ->
                    child != ignoredPath &&
                        child.fileName.toString() != expectedName &&
                        child.fileName.toString().equals(expectedName, ignoreCase = true)
                }
            }
            if (conflict != null) {
                throw CaseConflictException(
                    "路径大小写与现有文件冲突: ${displayPath(conflict)} / ${displayPath(parent.resolve(component))}",
                )
            }
            parent = parent.resolve(component)
        }
    }

    private fun validateExpectedState(target: Path, displayPath: String, expectedState: ExpectedPathState) {
        val exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS)
        val matches = when (expectedState) {
            ExpectedPathState.PRESENT -> exists
            ExpectedPathState.ABSENT -> !exists
        }
        if (!matches) {
            val expected = if (expectedState == ExpectedPathState.PRESENT) "存在" else "不存在"
            throw PreconditionFailedException("路径前置条件不满足: $displayPath 预期$expected")
        }
    }

    private fun ensureCapacityFor(additionalEntries: Int) {
        if (additionalEntries <= 0) return
        var entries = 0
        fun count(directory: Path, topLevel: Boolean) {
            listChildrenChecked(directory, filterAllowlist = topLevel).forEach { child ->
                if (Files.isSymbolicLink(child)) {
                    throw PolicyException("禁止访问符号链接: ${displayPath(child)}")
                }
                ensureRealPathInsideRoot(child)
                entries++
                if (entries + additionalEntries > maxTreeEntries) {
                    throw PolicyException("文件数量将超过限制: $maxTreeEntries")
                }
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    count(child, topLevel = false)
                }
            }
        }
        count(root, topLevel = true)
        if (entries + additionalEntries > maxTreeEntries) {
            throw PolicyException("文件数量将超过限制: $maxTreeEntries")
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
        private const val DEFAULT_MANIFEST_ID = "working-tree"
        private const val MAX_PATH_LENGTH = 1024
        private const val MAX_COMPONENT_LENGTH = 255
        private val SHA256_REVISION = Regex("^[0-9a-f]{64}$")
        private val DRIVE_PREFIX = Regex("^[A-Za-z]:.*")
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

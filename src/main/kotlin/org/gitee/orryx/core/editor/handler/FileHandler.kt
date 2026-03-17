package org.gitee.orryx.core.editor.handler

import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.editor.EditorClient
import taboolib.common.platform.function.getDataFolder
import java.io.File

/**
 * 文件操作处理器
 * 处理 file.list/read/write/create/delete/rename
 * 所有文件 I/O 在 ioScope 异步执行
 */
object FileHandler {

    private val rootDir: File get() = getDataFolder()

    /**
     * 路径安全检查：确保目标路径在插件数据目录内
     */
    private fun safePath(path: String): File? {
        val file = File(rootDir, path)
        val canonical = file.canonicalPath
        val rootCanonical = rootDir.canonicalPath
        return if (canonical.startsWith(rootCanonical)) file else null
    }

    /**
     * 递归构建文件树
     */
    private fun buildFileTree(dir: File, relativePath: String): List<JsonObject> {
        val children = dir.listFiles() ?: return emptyList()
        // 目录排在文件前面
        val sorted = children.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name })
        return sorted.map { file ->
            val path = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
            buildJsonObject {
                put("name", file.name)
                put("path", path)
                put("isDirectory", file.isDirectory)
                if (file.isDirectory) {
                    put("children", JsonArray(buildFileTree(file, path)))
                }
            }
        }
    }

    fun handleList(id: String, data: JsonObject?) {
        OrryxAPI.ioScope.launch {
            try {
                val path = data?.get("path")?.jsonPrimitive?.contentOrNull
                val dir = if (path.isNullOrEmpty()) rootDir else safePath(path)
                if (dir == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                if (!dir.exists() || !dir.isDirectory) {
                    EditorClient.sendError(id, "目录不存在: $path")
                    return@launch
                }
                val relativePath = if (path.isNullOrEmpty()) "" else path
                val tree = buildFileTree(dir, relativePath)
                EditorClient.sendMessage("file.tree", id, buildJsonObject {
                    put("files", JsonArray(tree))
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "列出文件失败: ${e.message}")
            }
        }
    }

    fun handleRead(id: String, data: JsonObject?) {
        val path = data?.get("path")?.jsonPrimitive?.contentOrNull
        if (path == null) {
            EditorClient.sendError(id, "缺少 path 字段")
            return
        }
        OrryxAPI.ioScope.launch {
            try {
                val file = safePath(path)
                if (file == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                if (!file.exists() || !file.isFile) {
                    EditorClient.sendError(id, "文件不存在: $path")
                    return@launch
                }
                val content = file.readText(Charsets.UTF_8)
                EditorClient.sendMessage("file.content", id, buildJsonObject {
                    put("path", path)
                    put("content", content)
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "读取文件失败: ${e.message}")
            }
        }
    }

    fun handleWrite(id: String, data: JsonObject?) {
        val path = data?.get("path")?.jsonPrimitive?.contentOrNull
        val content = data?.get("content")?.jsonPrimitive?.contentOrNull
        if (path == null || content == null) {
            EditorClient.sendError(id, "缺少 path 或 content 字段")
            return
        }
        OrryxAPI.ioScope.launch {
            try {
                val file = safePath(path)
                if (file == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
                EditorClient.sendMessage("file.written", id, buildJsonObject {
                    put("path", path)
                    put("success", true)
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "写入文件失败: ${e.message}")
            }
        }
    }

    fun handleCreate(id: String, data: JsonObject?) {
        val path = data?.get("path")?.jsonPrimitive?.contentOrNull
        val isDirectory = data?.get("isDirectory")?.jsonPrimitive?.boolean ?: false
        if (path == null) {
            EditorClient.sendError(id, "缺少 path 字段")
            return
        }
        OrryxAPI.ioScope.launch {
            try {
                val file = safePath(path)
                if (file == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                if (file.exists()) {
                    EditorClient.sendError(id, "文件已存在: $path")
                    return@launch
                }
                if (isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    file.createNewFile()
                }
                EditorClient.sendMessage("file.written", id, buildJsonObject {
                    put("path", path)
                    put("success", true)
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "创建文件失败: ${e.message}")
            }
        }
    }

    fun handleDelete(id: String, data: JsonObject?) {
        val path = data?.get("path")?.jsonPrimitive?.contentOrNull
        if (path == null) {
            EditorClient.sendError(id, "缺少 path 字段")
            return
        }
        OrryxAPI.ioScope.launch {
            try {
                val file = safePath(path)
                if (file == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                if (!file.exists()) {
                    EditorClient.sendError(id, "文件不存在: $path")
                    return@launch
                }
                val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                EditorClient.sendMessage("file.written", id, buildJsonObject {
                    put("path", path)
                    put("success", deleted)
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "删除文件失败: ${e.message}")
            }
        }
    }

    fun handleRename(id: String, data: JsonObject?) {
        val oldPath = data?.get("oldPath")?.jsonPrimitive?.contentOrNull
        val newPath = data?.get("newPath")?.jsonPrimitive?.contentOrNull
        if (oldPath == null || newPath == null) {
            EditorClient.sendError(id, "缺少 oldPath 或 newPath 字段")
            return
        }
        OrryxAPI.ioScope.launch {
            try {
                val oldFile = safePath(oldPath)
                val newFile = safePath(newPath)
                if (oldFile == null || newFile == null) {
                    EditorClient.sendError(id, "路径越界")
                    return@launch
                }
                if (!oldFile.exists()) {
                    EditorClient.sendError(id, "文件不存在: $oldPath")
                    return@launch
                }
                if (newFile.exists()) {
                    EditorClient.sendError(id, "目标已存在: $newPath")
                    return@launch
                }
                newFile.parentFile?.mkdirs()
                val renamed = oldFile.renameTo(newFile)
                EditorClient.sendMessage("file.written", id, buildJsonObject {
                    put("success", renamed)
                })
            } catch (e: Exception) {
                EditorClient.sendError(id, "重命名失败: ${e.message}")
            }
        }
    }
}

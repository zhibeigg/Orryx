package org.gitee.orryx.core.editor.handler

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.editor.EditorClient
import taboolib.common.platform.function.getDataFolder

/**
 * 文件操作处理器。
 *
 * 所有路径解析与文件系统操作统一交给 [EditorFilePolicy]，所有 I/O 在 ioScope 执行。
 */
object FileHandler {

    private val policy: EditorFilePolicy by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        EditorFilePolicy(getDataFolder().toPath())
    }
    private val operationMutex = Mutex()

    fun handleList(id: String, data: JsonObject?) {
        val path = data.string("path")
        launchOperation(id, "列出文件失败") {
            val tree = policy.listTree(path)
            EditorClient.sendMessage("file.tree", id, buildJsonObject {
                put("files", JsonArray(tree.map(::toJson)))
            })
        }
    }

    fun handleRead(id: String, data: JsonObject?) {
        val path = data.requireString(id, "path") ?: return
        launchOperation(id, "读取文件失败") {
            val content = policy.readText(path)
            EditorClient.sendMessage("file.content", id, buildJsonObject {
                put("path", path)
                put("content", content)
            })
        }
    }

    fun handleWrite(id: String, data: JsonObject?) {
        val path = data.requireString(id, "path") ?: return
        val content = data.requireString(id, "content") ?: return
        launchOperation(id, "写入文件失败") {
            policy.writeTextAtomic(path, content)
            sendWritten(id, path, true)
        }
    }

    fun handleCreate(id: String, data: JsonObject?) {
        val path = data.requireString(id, "path") ?: return
        val isDirectory = data?.get("isDirectory")?.jsonPrimitive?.booleanOrNull ?: false
        launchOperation(id, "创建文件失败") {
            policy.create(path, isDirectory)
            sendWritten(id, path, true)
        }
    }

    fun handleDelete(id: String, data: JsonObject?) {
        val path = data.requireString(id, "path") ?: return
        launchOperation(id, "删除文件失败") {
            sendWritten(id, path, policy.delete(path))
        }
    }

    fun handleRename(id: String, data: JsonObject?) {
        val oldPath = data.requireString(id, "oldPath") ?: return
        val newPath = data.requireString(id, "newPath") ?: return
        launchOperation(id, "重命名失败") {
            val renamed = policy.rename(oldPath, newPath)
            EditorClient.sendMessage("file.written", id, buildJsonObject {
                put("oldPath", oldPath)
                put("path", newPath)
                put("success", renamed)
            })
        }
    }

    private fun launchOperation(id: String, operation: String, block: () -> Unit) {
        OrryxAPI.ioScope.launch {
            operationMutex.withLock {
                try {
                    block()
                } catch (e: EditorFilePolicy.PolicyException) {
                    EditorClient.sendError(id, e.message ?: operation)
                } catch (e: Exception) {
                    EditorClient.sendError(id, "$operation: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun sendWritten(id: String, path: String, success: Boolean) {
        EditorClient.sendMessage("file.written", id, buildJsonObject {
            put("path", path)
            put("success", success)
        })
    }

    private fun toJson(entry: EditorFilePolicy.TreeEntry): JsonObject {
        return buildJsonObject {
            put("name", entry.name)
            put("path", entry.path)
            put("isDirectory", entry.isDirectory)
            if (entry.isDirectory) {
                put("children", JsonArray(entry.children.map(::toJson)))
            }
        }
    }

    private fun JsonObject?.string(key: String): String? {
        return this?.get(key)?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject?.requireString(id: String, key: String): String? {
        return string(key)?.also {
            if (it.isEmpty() && key != "content") {
                EditorClient.sendError(id, "$key 字段不能为空")
            }
        }?.takeUnless { it.isEmpty() && key != "content" } ?: run {
            if (string(key) == null) {
                EditorClient.sendError(id, "缺少 $key 字段")
            }
            null
        }
    }
}

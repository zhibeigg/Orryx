package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.gitee.orryx.core.editor.EditorClient
import taboolib.common.platform.function.getDataFolder

/**
 * 文件操作处理器。
 *
 * 所有路径解析与文件系统操作统一交给 [EditorFilePolicy]，并通过 [EditorRequestQueue] 在 I/O 协程域内
 * 有界、顺序执行。每个请求和响应均绑定接收请求时的连接 generation。
 */
object FileHandler {

    private val SHA256_REVISION = Regex("^[0-9a-f]{64}$")
    private val policy: EditorFilePolicy by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        EditorFilePolicy(getDataFolder().toPath())
    }

    fun handleList(generation: Long, id: String, data: JsonObject?) {
        val path = data.string("path")
        EditorRequestQueue.enqueue(generation, id, "列出文件失败") { requestGeneration ->
            val tree = policy.listTree(path)
            EditorClient.sendMessage(requestGeneration, "file.tree", id, buildJsonObject {
                put("files", JsonArray(tree.map(::toJson)))
            })
        }
    }

    fun handleRead(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        EditorRequestQueue.enqueue(generation, id, "读取文件失败") { requestGeneration ->
            val file = policy.readTextWithRevision(path)
            EditorClient.sendMessage(requestGeneration, "file.content", id, buildJsonObject {
                put("path", path)
                put("content", file.content)
                put("revision", file.revision)
            })
        }
    }

    fun handleWrite(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        val content = data.requireString(generation, id, "content") ?: return
        val expectedRevision = data.string("expectedRevision")
        val force = (data?.get("force") as? JsonPrimitive)?.booleanOrNull ?: false
        if (EditorClient.isProtocolV2(generation) && !force && expectedRevision.isNullOrBlank()) {
            EditorClient.sendError(generation, id, "V2 file.write 必须提供 expectedRevision，除非 force=true")
            return
        }
        if (EditorClient.isProtocolV2(generation) && expectedRevision != null && !SHA256_REVISION.matches(expectedRevision)) {
            EditorClient.sendError(generation, id, "expectedRevision 必须是 64 位小写 SHA-256")
            return
        }
        EditorRequestQueue.enqueue(generation, id, "写入文件失败") { requestGeneration ->
            val revision = policy.writeTextAtomic(path, content, expectedRevision.takeUnless { force })
            sendWritten(requestGeneration, id, path, true, revision)
        }
    }

    fun handleCreate(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        val isDirectory = (data?.get("isDirectory") as? JsonPrimitive)?.booleanOrNull ?: false
        val expectedState = data.expectedState("expectedState", "expectedAbsent")
            ?: ExpectedPathState.ABSENT.takeIf { EditorClient.isProtocolV2(generation) }
        EditorRequestQueue.enqueue(generation, id, "创建文件失败") { requestGeneration ->
            policy.create(path, isDirectory, expectedState)
            sendWritten(requestGeneration, id, path, true)
        }
    }

    fun handleDelete(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        val expectedState = data.expectedState("expectedState", "expectedPresent")
            ?: ExpectedPathState.PRESENT.takeIf { EditorClient.isProtocolV2(generation) }
        EditorRequestQueue.enqueue(generation, id, "删除文件失败") { requestGeneration ->
            sendWritten(requestGeneration, id, path, policy.delete(path, expectedState))
        }
    }

    fun handleRename(generation: Long, id: String, data: JsonObject?) {
        val oldPath = data.requireString(generation, id, "oldPath") ?: return
        val newPath = data.requireString(generation, id, "newPath") ?: return
        val expectedSourceState = data.expectedState("expectedSourceState", "expectedSourcePresent")
            ?: ExpectedPathState.PRESENT.takeIf { EditorClient.isProtocolV2(generation) }
        val expectedTargetState = data.expectedState("expectedTargetState", "expectedTargetAbsent")
            ?: ExpectedPathState.ABSENT.takeIf { EditorClient.isProtocolV2(generation) }
        EditorRequestQueue.enqueue(generation, id, "重命名失败") { requestGeneration ->
            val renamed = policy.rename(oldPath, newPath, expectedSourceState, expectedTargetState)
            EditorClient.sendMessage(requestGeneration, "file.written", id, buildJsonObject {
                put("oldPath", oldPath)
                put("path", newPath)
                put("success", renamed)
            })
        }
    }

    private fun sendWritten(
        generation: Long,
        id: String,
        path: String,
        success: Boolean,
        revision: String? = null,
    ) {
        EditorClient.sendMessage(generation, "file.written", id, buildJsonObject {
            put("path", path)
            put("success", success)
            revision?.let { put("revision", it) }
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
        return (this?.get(key) as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonObject?.expectedState(stateKey: String, booleanKey: String): ExpectedPathState? {
        when (string(stateKey)?.lowercase()) {
            "present" -> return ExpectedPathState.PRESENT
            "absent" -> return ExpectedPathState.ABSENT
        }
        val booleanValue = (this?.get(booleanKey) as? JsonPrimitive)?.booleanOrNull ?: return null
        val keyExpectsAbsent = booleanKey.endsWith("Absent")
        return when {
            booleanValue && keyExpectsAbsent -> ExpectedPathState.ABSENT
            booleanValue -> ExpectedPathState.PRESENT
            keyExpectsAbsent -> ExpectedPathState.PRESENT
            else -> ExpectedPathState.ABSENT
        }
    }

    private fun JsonObject?.requireString(generation: Long, id: String, key: String): String? {
        return string(key)?.also {
            if (it.isEmpty() && key != "content") {
                EditorClient.sendError(generation, id, "$key 字段不能为空")
            }
        }?.takeUnless { it.isEmpty() && key != "content" } ?: run {
            if (string(key) == null) {
                EditorClient.sendError(generation, id, "缺少 $key 字段")
            }
            null
        }
    }
}

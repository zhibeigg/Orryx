package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.gitee.orryx.core.editor.EditorClient
import org.gitee.orryx.core.editor.EditorProtocol
import org.gitee.orryx.core.editor.EditorSafeError
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
        val protocolV2 = EditorClient.isProtocolV2(generation)
        if (protocolV2 && !requireV2WriteCapability(generation, id)) return
        val force = (data?.get("force") as? JsonPrimitive)?.booleanOrNull ?: false
        val expectedRevision = try {
            resolveWriteRevision(
                expectedRevision = data.string("expectedRevision"),
                baseRevision = data.string("baseRevision"),
                protocolV2 = protocolV2,
                force = force,
            )
        } catch (failure: WriteRevisionException) {
            EditorClient.sendError(generation, id, EditorSafeError(failure.code, failure.message ?: "写入前置版本无效"))
            return
        }
        EditorRequestQueue.enqueue(
            generation,
            id,
            "写入文件失败",
            EditorMutationOperation.FILE_WRITE,
        ) { requestGeneration ->
            val revision = policy.writeTextAtomic(path, content, expectedRevision)
            sendWritten(requestGeneration, id, path, true, revision.takeIf { protocolV2 })
        }
    }

    fun handleCreate(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        val protocolV2 = EditorClient.isProtocolV2(generation)
        if (protocolV2 && !requireV2WriteCapability(generation, id)) return
        val isDirectory = (data?.get("isDirectory") as? JsonPrimitive)?.booleanOrNull ?: false
        val expectedState = data.expectedState("expectedState", "expectedAbsent")
            ?: ExpectedPathState.ABSENT.takeIf { protocolV2 }
        EditorRequestQueue.enqueue(
            generation,
            id,
            "创建文件失败",
            EditorMutationOperation.FILE_CREATE,
        ) { requestGeneration ->
            val revision = policy.create(path, isDirectory, expectedState)
            sendWritten(requestGeneration, id, path, true, revision.takeIf { protocolV2 })
        }
    }

    fun handleDelete(generation: Long, id: String, data: JsonObject?) {
        val path = data.requireString(generation, id, "path") ?: return
        val protocolV2 = EditorClient.isProtocolV2(generation)
        if (protocolV2 && !requireV2WriteCapability(generation, id)) return
        val expectedState = data.expectedState("expectedState", "expectedPresent")
            ?: ExpectedPathState.PRESENT.takeIf { protocolV2 }
        EditorRequestQueue.enqueue(
            generation,
            id,
            "删除文件失败",
            EditorMutationOperation.FILE_DELETE,
        ) { requestGeneration ->
            sendWritten(requestGeneration, id, path, policy.delete(path, expectedState))
        }
    }

    fun handleRename(generation: Long, id: String, data: JsonObject?) {
        val oldPath = data.requireString(generation, id, "oldPath") ?: return
        val newPath = data.requireString(generation, id, "newPath") ?: return
        val protocolV2 = EditorClient.isProtocolV2(generation)
        if (protocolV2 && !requireV2WriteCapability(generation, id)) return
        val expectedSourceState = data.expectedState("expectedSourceState", "expectedSourcePresent")
            ?: ExpectedPathState.PRESENT.takeIf { protocolV2 }
        val expectedTargetState = data.expectedState("expectedTargetState", "expectedTargetAbsent")
            ?: ExpectedPathState.ABSENT.takeIf { protocolV2 }
        EditorRequestQueue.enqueue(
            generation,
            id,
            "重命名失败",
            EditorMutationOperation.FILE_RENAME,
        ) { requestGeneration ->
            val renamed = policy.rename(oldPath, newPath, expectedSourceState, expectedTargetState)
            EditorClient.sendMessage(requestGeneration, "file.written", id, buildJsonObject {
                put("oldPath", oldPath)
                put("path", newPath)
                put("success", renamed)
            })
        }
    }

    internal class WriteRevisionException(
        val code: String,
        override val message: String,
    ) : IllegalArgumentException(message)

    internal fun resolveWriteRevision(
        expectedRevision: String?,
        baseRevision: String?,
        protocolV2: Boolean,
        force: Boolean,
    ): String? {
        if (!protocolV2) return null
        if (expectedRevision != null && baseRevision != null && expectedRevision != baseRevision) {
            throw WriteRevisionException(
                "REVISION_FIELDS_MISMATCH",
                "expectedRevision 与 baseRevision 不一致",
            )
        }
        val revision = expectedRevision ?: baseRevision
        if (protocolV2 && !force && revision.isNullOrBlank()) {
            throw WriteRevisionException(
                "REVISION_REQUIRED",
                "V2 file.write 必须提供 expectedRevision 或 baseRevision，除非 force=true",
            )
        }
        if (protocolV2 && revision != null && !SHA256_REVISION.matches(revision)) {
            throw WriteRevisionException(
                "INVALID_REVISION",
                "revision 必须是 64 位小写 SHA-256",
            )
        }
        return revision.takeUnless { force }
    }

    private fun requireV2WriteCapability(generation: Long, id: String): Boolean {
        if (EditorProtocol.RELAY_WRITE_CAPABILITY in EditorClient.currentRelayCapabilities()) return true
        EditorClient.sendError(
            generation,
            id,
            "relay 未声明 ${EditorProtocol.RELAY_WRITE_CAPABILITY}",
            "RELAY_CAPABILITY_MISSING",
        )
        return false
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

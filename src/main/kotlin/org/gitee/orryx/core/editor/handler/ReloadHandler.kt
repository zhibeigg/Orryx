package org.gitee.orryx.core.editor.handler

import kotlinx.coroutines.future.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.gitee.orryx.core.editor.EditorClient
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.utils.mainThreadFuture

/**
 * reload 消息处理器。
 *
 * 当前 ReloadAPI 只支持完整重载。请求通过 Editor 公共顺序队列排队，再以非阻塞方式切回主线程执行。
 */
object ReloadHandler {

    fun handle(generation: Long, id: String, data: JsonObject?) {
        val module = ((data?.get("module") as? JsonPrimitive)?.contentOrNull ?: "all").trim().ifEmpty { "all" }
        EditorRequestQueue.enqueue(
            generation,
            id,
            "重载失败",
            EditorMutationOperation.RELOAD,
        ) { requestGeneration ->
            if (!module.equals("all", ignoreCase = true)) {
                sendResult(requestGeneration, id, module, false, "不支持局部重载，仅支持 module=all")
                return@enqueue
            }
            try {
                val report = mainThreadFuture { ReloadAPI.reloadWithReport() }.await()
                sendResult(requestGeneration, id, "all", report.success, report.summary())
            } catch (e: Exception) {
                sendResult(
                    requestGeneration,
                    id,
                    "all",
                    false,
                    "重载失败: ${e.message ?: e.javaClass.simpleName}",
                )
            }
        }
    }

    private fun sendResult(
        generation: Long,
        id: String,
        module: String,
        success: Boolean,
        message: String,
    ) {
        EditorClient.sendMessage(generation, "reload.result", id, buildJsonObject {
            put("module", module)
            put("success", success)
            put("message", message)
        })
    }
}

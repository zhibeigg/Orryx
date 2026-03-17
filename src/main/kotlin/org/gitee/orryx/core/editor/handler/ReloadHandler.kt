package org.gitee.orryx.core.editor.handler

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.gitee.orryx.core.editor.EditorClient
import org.gitee.orryx.core.reload.ReloadAPI
import taboolib.common.platform.function.submit

/**
 * reload 消息处理器
 * 切回主线程调用 ReloadAPI.reload()
 * 支持 module 参数
 */
object ReloadHandler {

    fun handle(id: String, data: JsonObject?) {
        val module = data?.get("module")?.jsonPrimitive?.content ?: "all"
        // reload 必须回主线程
        submit {
            try {
                ReloadAPI.reload()
                EditorClient.sendMessage("reload.result", id, buildJsonObject {
                    put("module", module)
                    put("success", true)
                    put("message", "已重载")
                })
            } catch (e: Exception) {
                EditorClient.sendMessage("reload.result", id, buildJsonObject {
                    put("module", module)
                    put("success", false)
                    put("message", "重载失败: ${e.message}")
                })
            }
        }
    }
}

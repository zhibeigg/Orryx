package org.gitee.orryx.core.editor

import org.gitee.orryx.core.common.NanoId
import java.util.concurrent.CompletableFuture

/** 编辑器 Token 管理器。 */
object EditorTokenManager {

    internal const val CENTER_HOST = "orryx.mcwar.cn"
    internal const val PUBLIC_URL = "https://$CENTER_HOST"
    internal const val SERVER_URL = "wss://$CENTER_HOST/ws/server"
    internal const val CONSOLE_ACTOR = "CONSOLE"

    /** 只有固定中心确认 Token 注册成功后才返回 URL，actorName 可以是玩家名或控制台标识。 */
    fun generateEditorUrl(actorName: String): CompletableFuture<String?> {
        if (!EditorClient.isRegistered()) return CompletableFuture.completedFuture(null)
        val generation = EditorClient.currentGeneration()
        val token = NanoId.generate(size = 16)
        return EditorClient.registerToken(token, actorName).thenApply { registered ->
            if (!registered || !EditorClient.isRegistered() || EditorClient.currentGeneration() != generation) {
                return@thenApply null
            }
            buildEditorUrl(token)
        }
    }

    internal fun buildEditorUrl(token: String): String = "$PUBLIC_URL/connect#token=$token"
}

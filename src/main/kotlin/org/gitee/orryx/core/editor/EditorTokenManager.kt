package org.gitee.orryx.core.editor

import org.gitee.orryx.core.common.NanoId
import java.util.concurrent.CompletableFuture

/** 编辑器 Token 管理器。 */
object EditorTokenManager {

    /** 只有中心服务器确认 Token 注册成功后才返回 URL。 */
    fun generateEditorUrl(playerName: String): CompletableFuture<String?> {
        if (!EditorClient.isRegistered()) return CompletableFuture.completedFuture(null)
        val token = NanoId.generate(size = 16)
        return EditorClient.registerToken(token, playerName).thenApply { registered ->
            if (!registered) return@thenApply null
            val baseUrl = EditorClient.getEditorUrl().trimEnd('/')
            "$baseUrl/?token=$token"
        }
    }
}

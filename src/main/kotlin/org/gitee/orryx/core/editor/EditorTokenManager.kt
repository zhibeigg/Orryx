package org.gitee.orryx.core.editor

import org.gitee.orryx.core.common.NanoId

/**
 * 编辑器 Token 管理器
 * 生成 Token 并注册到中心服务器
 */
object EditorTokenManager {

    /**
     * 生成 Token 并注册到中心服务器，返回编辑器 URL
     * @param playerName 请求编辑器的玩家名
     * @return 编辑器 URL，如果未连接中心服务器则返回 null
     */
    fun generateEditorUrl(playerName: String): String? {
        if (!EditorClient.isConnected()) return null
        val token = NanoId.generate(size = 16)
        EditorClient.registerToken(token, playerName)
        val baseUrl = EditorClient.getEditorUrl().trimEnd('/')
        return "$baseUrl/?token=$token"
    }
}

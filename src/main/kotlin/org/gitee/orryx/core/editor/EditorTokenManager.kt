package org.gitee.orryx.core.editor

import org.gitee.orryx.core.common.NanoId
import java.net.URI
import java.util.concurrent.CompletableFuture

/** 编辑器 Token 管理器。 */
object EditorTokenManager {

    internal const val DEFAULT_PUBLIC_URL = "https://orryx.mcwar.cn"

    /** 只有 PublicUrl 合法且中心服务器确认 Token 注册成功后才返回 URL。 */
    fun generateEditorUrl(playerName: String): CompletableFuture<String?> {
        val publicUrl = EditorClient.getEditorUrl() ?: return CompletableFuture.completedFuture(null)
        if (!EditorClient.isRegistered()) return CompletableFuture.completedFuture(null)
        val generation = EditorClient.currentGeneration()
        val token = NanoId.generate(size = 16)
        return EditorClient.registerToken(token, playerName).thenApply { registered ->
            if (!registered || !EditorClient.isRegistered() || EditorClient.currentGeneration() != generation) {
                return@thenApply null
            }
            buildEditorUrl(publicUrl, token)
        }
    }

    internal fun normalizePublicUrl(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (
            !uri.isAbsolute ||
            uri.isOpaque ||
            !uri.scheme.equals("https", ignoreCase = true) ||
            uri.host.isNullOrEmpty() ||
            uri.rawUserInfo != null ||
            uri.rawQuery != null ||
            uri.rawFragment != null
        ) {
            return null
        }
        val normalized = uri.normalize().toASCIIString().trimEnd('/')
        return "https:${normalized.substringAfter(':')}"
    }

    internal fun buildEditorUrl(baseUrl: String, token: String): String? {
        val publicUrl = normalizePublicUrl(baseUrl) ?: return null
        return "$publicUrl/connect#token=$token"
    }
}

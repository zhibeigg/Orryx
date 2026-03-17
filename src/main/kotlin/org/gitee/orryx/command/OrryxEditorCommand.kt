package org.gitee.orryx.command

import org.bukkit.entity.Player
import org.gitee.orryx.core.editor.EditorTokenManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendLang

object OrryxEditorCommand {

    @CommandBody(permission = "Orryx.Command.Editor")
    val token = subCommand {
        exec<ProxyCommandSender> {
            val player = sender.castSafely<Player>()
            if (player == null) {
                sender.sendMessage("§c此命令只能由玩家执行")
                return@exec
            }
            val url = EditorTokenManager.generateEditorUrl(player.name)
            if (url == null) {
                sender.sendLang("editor-not-connected")
                return@exec
            }
            sender.sendLang("editor-open", url)
        }
    }
}

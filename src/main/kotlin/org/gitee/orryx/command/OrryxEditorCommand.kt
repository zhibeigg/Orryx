package org.gitee.orryx.command

import org.gitee.orryx.core.editor.EditorTokenManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendLang

object OrryxEditorCommand {

    @CommandBody(permission = "Orryx.Command.Editor")
    val token = subCommand {
        exec<ProxyCommandSender> {
            val name = sender.name.ifEmpty { "Console" }
            val url = EditorTokenManager.generateEditorUrl(name)
            if (url == null) {
                sender.sendLang("editor-not-connected")
                return@exec
            }
            sender.sendLang("editor-open", url)
        }
    }
}

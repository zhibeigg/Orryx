package org.gitee.orryx.command

import org.bukkit.Bukkit
import org.gitee.orryx.core.ui.IUIManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand

object OrryxUICommand {

    @CommandBody
    val bindSkill = subCommand {
        player("owner") {
            player("viewer") {
                exec<ProxyCommandSender> {
                    val owner = Bukkit.getPlayerExact(ctx["owner"]) ?: return@exec
                    val viewer = Bukkit.getPlayerExact(ctx["viewer"]) ?: return@exec
                    IUIManager.INSTANCE.getSkillUI(viewer, owner).open()
                }
            }
        }
    }

}
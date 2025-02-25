package org.gitee.orryx.command

import org.bukkit.Bukkit
import org.gitee.orryx.core.ui.IUIManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendLang

object OrryxUICommand {

    @CommandBody
    val openUI = subCommand {
        player("owner") {
            player("viewer") {
                exec<ProxyCommandSender> {
                    val owner = Bukkit.getPlayerExact(ctx["owner"]) ?: return@exec
                    val viewer = Bukkit.getPlayerExact(ctx["viewer"]) ?: return@exec
                    IUIManager.INSTANCE.createSkillUI(viewer, owner).open()
                }
            }
        }
    }

    @CommandBody
    val openHud = subCommand {
        player("owner") {
            player("viewer") {
                exec<ProxyCommandSender> {
                    val owner = Bukkit.getPlayerExact(ctx["owner"]) ?: return@exec
                    val viewer = Bukkit.getPlayerExact(ctx["viewer"]) ?: return@exec
                    IUIManager.INSTANCE.createSkillHUD(viewer, owner).open()
                }
            }
        }
    }

    @CommandBody
    val hudOwner = subCommand {
        player("viewer") {
            exec<ProxyCommandSender> {
                val viewer = Bukkit.getPlayerExact(ctx["viewer"]) ?: return@exec
                sender.sendLang("command-hud-owner", IUIManager.INSTANCE.getSkillHUD(viewer)?.owner?.name ?: "无")
            }
        }
    }

}
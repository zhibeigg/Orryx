package org.gitee.orryx.command

import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.utils.bukkitPlayer
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.int
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common5.cint

object OrryxPlayerNavigationCommand {

    @CommandBody
    val goto: subCommand = subCommand {
        player {
            int("x") {
                int("y") {
                    int("z") {
                        int("range") {
                            exec<ProxyCommandSender> {
                                val player = ctx.bukkitPlayer() ?: return@exec
                                val x = ctx["x"].cint
                                val y = ctx["y"].cint
                                val z = ctx["z"].cint
                                val r = ctx["range"].cint
                                PluginMessageHandler.playerNavigation(player, x, y, z, r)
                            }
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val stop: subCommand = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                PluginMessageHandler.stopPlayerNavigation(player)
            }
        }
    }
}
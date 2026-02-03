package org.gitee.orryx.command

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.utils.bukkitPlayer
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.int
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common5.cint

object OrryxSilenceCommand {

    @CommandBody
    val give = subCommand {
        player {
            int("silence") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    Orryx.api().profileAPI.addSilence(player, ctx["silence"].cint.toLong())
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            int("silence") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    Orryx.api().profileAPI.reduceSilence(player, ctx["silence"].cint.toLong())
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            int("silence") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    Orryx.api().profileAPI.setSilence(player, ctx["silence"].cint.toLong())
                }
            }
        }
    }

    @CommandBody
    val cancel = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                Orryx.api().profileAPI.cancelSilence(player)
            }
        }
    }
}
package org.gitee.orryx.command

import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.debug
import org.gitee.orryx.utils.job
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.int
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common5.cint

object OrryxJobLevelCommand {

    @CommandBody
    val give = subCommand {
        player {
            int("level") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job()?.giveLevel(ctx["level"].cint)?.whenComplete { t, _ ->
                        debug("${player.name}指令job level give结果${t}")
                    }
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            int("level") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job()?.takeLevel(ctx["level"].cint)?.whenComplete { t, _ ->
                        debug("${player.name}指令job level take结果${t}")
                    }
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            int("level") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job()?.setLevel(ctx["level"].cint)?.whenComplete { t, _ ->
                        debug("${player.name}指令job level set结果${t}")
                    }
                }
            }
        }
    }

}
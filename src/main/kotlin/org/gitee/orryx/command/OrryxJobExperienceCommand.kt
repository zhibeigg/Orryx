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

object OrryxJobExperienceCommand {

    @CommandBody
    val give = subCommand {
        player {
            int("experience") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    debug("${player.name}指令job experience give结果${player.job()?.giveExperience(ctx["experience"].cint)}")
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            int("experience") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    debug("${player.name}指令job experience take结果${player.job()?.takeExperience(ctx["experience"].cint)}")
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            int("experience") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    debug("${player.name}指令job experience set结果${player.job()?.setExperience(ctx["experience"].cint)}")
                }
            }
        }
    }

}
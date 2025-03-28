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
                    player.job {
                        it.giveExperience(ctx["experience"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} 职业${it.key}获取经验成功")
                            debug("${player.name}指令job experience give结果${t}")
                        }
                    }
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
                    player.job {
                        it.takeExperience(ctx["experience"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} 职业${it.key}减少经验成功")
                            debug("${player.name}指令job experience take结果${t}")
                        }
                    }
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
                    player.job {
                        it.setExperience(ctx["experience"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} 职业${it.key}设置经验成功")
                            debug("${player.name}指令job experience set结果${t}")
                        }
                    }
                }
            }
        }
    }

}
package org.gitee.orryx.command

import org.gitee.orryx.core.skill.SkillLoaderManager.getSkills
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.debug
import org.gitee.orryx.utils.skill
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint

object OrryxSkillLevelCommand {

    @CommandBody
    val give = subCommand {
        player {
            dynamic("skill") {
                suggest { getSkills().map { it.key } }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        player.skill(ctx["skill"]) {
                            it.upLevel(ctx["level"].cint).whenComplete { t, _ ->
                                sender.sendMessage("玩家${player.name} result: $t")
                                debug("${player.name}指令skill level give结果${t}")
                            }
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val take = subCommand {
        player {
            dynamic("skill") {
                suggest { getSkills().map { it.key } }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        player.skill(ctx["skill"]) {
                            it.downLevel(ctx["level"].cint).whenComplete { t, _ ->
                                sender.sendMessage("玩家${player.name} result: $t")
                                debug("${player.name}指令skill level take结果${t}")
                            }
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val set = subCommand {
        player {
            dynamic("skill") {
                suggest { getSkills().map { it.key } }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        player.skill(ctx["skill"]) {
                            it.setLevel(ctx["level"].cint).whenComplete { t, _ ->
                                sender.sendMessage("玩家${player.name} result: $t")
                                debug("${player.name}指令skill level set结果${t}")
                            }
                        }
                    }
                }
            }
        }
    }


}
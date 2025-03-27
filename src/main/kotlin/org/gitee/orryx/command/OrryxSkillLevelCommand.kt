package org.gitee.orryx.command

import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.debug
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint

object OrryxSkillLevelCommand {

    @CommandBody
    val give = subCommand {
        player {
            dynamic("skill") {
                suggest {
                    val player = ctx.bukkitPlayer() ?: return@suggest emptyList()
                    val job = player.orryxProfile().job?.let { JobLoaderManager.getJobLoader(it) } ?: return@suggest emptyList()
                    job.skills
                }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        val skill = player.getSkill(player.orryxProfile().job!!, ctx["skill"]) ?: return@exec
                        skill.upLevel(ctx["level"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} result: $t")
                            debug("${player.name}指令skill level give结果${t}")
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
                suggest {
                    val player = ctx.bukkitPlayer() ?: return@suggest emptyList()
                    val job = player.orryxProfile().job?.let { JobLoaderManager.getJobLoader(it) } ?: return@suggest emptyList()
                    job.skills
                }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        val skill = player.getSkill(player.orryxProfile().job!!, ctx["skill"]) ?: return@exec
                        skill.downLevel(ctx["level"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} result: $t")
                            debug("${player.name}指令skill level take结果${t}")
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
                suggest {
                    val player = ctx.bukkitPlayer() ?: return@suggest emptyList()
                    val job = player.orryxProfile().job?.let { JobLoaderManager.getJobLoader(it) } ?: return@suggest emptyList()
                    job.skills
                }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        val skill = player.getSkill(player.orryxProfile().job!!, ctx["skill"]) ?: return@exec
                        skill.setLevel(ctx["level"].cint).whenComplete { t, _ ->
                            sender.sendMessage("玩家${player.name} result: $t")
                            debug("${player.name}指令skill level set结果${t}")
                        }
                    }
                }
            }
        }
    }


}
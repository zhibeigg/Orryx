package org.gitee.orryx.command

import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.profile.PlayerProfileManager.orryxProfile
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cint

object OrryxSkillCommand {

    @CommandBody
    val bindKey = subCommand {
        player {
            dynamic("skill") {
                suggest {
                    val player = ctx.bukkitPlayer() ?: return@suggest emptyList()
                    player.getSkills().map { it.key }
                }
                dynamic("group") {
                    suggest { BindKeyLoaderManager.getGroups().keys.toList() }
                    dynamic("key") {
                        suggest { BindKeyLoaderManager.getBindKeys().values.sortedBy { it.sort }.map { it.key } }
                        exec<ProxyCommandSender> {
                            val player = ctx.bukkitPlayer() ?: return@exec
                            val job = player.job()
                            val skill = player.getSkill(job?.key ?: return@exec, ctx["skill"]) ?: return@exec
                            val group = BindKeyLoaderManager.getGroup(ctx["group"]) ?: return@exec
                            val bindKey = BindKeyLoaderManager.getBindKey(ctx["key"]) ?: return@exec
                            debug("${player.name}指令skill bindKey结果${job.setBindKey(skill, group, bindKey)}")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val cast = subCommand {
        player {
            dynamic("skill") {
                suggest { SkillLoaderManager.getSkills().filter { it.value is ICastSkill }.map { it.key } }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        val skill = SkillLoaderManager.getSkillLoader(ctx["skill"]) as ICastSkill
                        skill.castSkill(player, SkillParameter(skill.key, player, ctx["level"].cint), false)
                    }
                }
            }
        }
    }

    @CommandBody
    val tryCast = subCommand {
        player {
            dynamic("skill") {
                suggest {
                    val player = ctx.bukkitPlayer() ?: return@suggest emptyList()
                    player.getSkills().map { it.key }
                }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    val skill = player.getSkill(player.orryxProfile().job!!, ctx["skill"]) ?: return@exec
                    skill.tryCast()
                }
            }
        }
    }

    @CommandBody
    val level = OrryxSkillLevelCommand

}
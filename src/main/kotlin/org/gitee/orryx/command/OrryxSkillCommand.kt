package org.gitee.orryx.command

import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.key.BindKeyLoaderManager
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common5.cbool
import taboolib.common5.cint

object OrryxSkillCommand {

    @CommandBody
    val bindKey = subCommand {
        player {
            dynamic("skill") {
                suggest { SkillLoaderManager.getSkills().map { it.key } }
                dynamic("group") {
                    suggest { BindKeyLoaderManager.getGroups().keys.toList() }
                    dynamic("key") {
                        suggest { BindKeyLoaderManager.getBindKeys().values.sortedBy { it.sort }.map { it.key } }
                        exec<ProxyCommandSender> {
                            val player = ctx.bukkitPlayer() ?: return@exec
                            player.job { job ->
                                player.getSkill(ctx["skill"]).thenApply {
                                    val group = BindKeyLoaderManager.getGroup(ctx["group"]) ?: return@thenApply
                                    val bindKey = BindKeyLoaderManager.getBindKey(ctx["key"]) ?: return@thenApply
                                    job.setBindKey(it ?: return@thenApply, group, bindKey).whenComplete { t, _ ->
                                        sender.sendMessage("玩家${player.name} 绑定按键 result: $t")
                                        debug("${player.name}指令skill bindKey结果${t}")
                                    }
                                }
                            }
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
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    val skill = SkillLoaderManager.getSkillLoader(ctx["skill"]) as ICastSkill
                    skill.castSkill(player, SkillParameter(skill.key, player, 1), false)
                }
                int("level") {
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        val skill = SkillLoaderManager.getSkillLoader(ctx["skill"]) as ICastSkill
                        skill.castSkill(player, SkillParameter(skill.key, player, ctx["level"].cint), false)
                    }
                    bool("consume") {
                        exec<ProxyCommandSender> {
                            val player = ctx.bukkitPlayer() ?: return@exec
                            val skill = SkillLoaderManager.getSkillLoader(ctx["skill"]) as ICastSkill
                            val consume = ctx["consume"].cbool
                            skill.castSkill(player, SkillParameter(skill.key, player, ctx["level"].cint), consume)
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val tryCast = subCommand {
        player {
            dynamic("skill") {
                suggest { SkillLoaderManager.getSkills().map { it.key } }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.getSkill(ctx["skill"]).thenApply {
                        it?.tryCast()
                    }
                }
            }
        }
    }

    @CommandBody
    val clear = subCommand {
        player {
            dynamic("skill") {
                suggest { SkillLoaderManager.getSkills().map { it.key } }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.getSkill(ctx["skill"]).thenApply {
                        (it ?: return@thenApply).clear().whenComplete { _, _ ->
                            sender.sendMessage("技能${it.key}已清除")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val level = OrryxSkillLevelCommand
}
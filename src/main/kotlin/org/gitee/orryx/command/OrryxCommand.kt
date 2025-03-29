package org.gitee.orryx.command

import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.mana.IManaManager
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common.platform.function.info
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang

@CommandHeader("Orryx", ["or"], "Orryx技能插件主指令", permission = "Orryx.Command.Main", permissionMessage = "你没有权限使用此指令")
object OrryxCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val reload = subCommandExec<ProxyCommandSender> {
        ReloadAPI.reload()
        sender.sendMessage("Orryx重载成功")
    }

    @CommandBody
    val job = OrryxJobCommand

    @CommandBody
    val mana = OrryxManaCommand

    @CommandBody
    val point = OrryxPointCommand

    @CommandBody
    val skill = OrryxSkillCommand

    @CommandBody
    val ui = OrryxUICommand

    @CommandBody
    val script = OrryxScriptCommand

    @CommandBody
    val info = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                player.orryxProfile { profile ->
                    val jobKey = profile.job
                    if (jobKey != null) {
                        IManaManager.INSTANCE.getMana(player).thenApply { mana ->
                            IManaManager.INSTANCE.getMaxMana(player).thenApply { maxMana ->
                                player.job(jobKey) { job ->
                                    sender.sendLang(
                                        "info",
                                        player.name,
                                        job.job.name,
                                        job.key,
                                        job.job.skills.size,
                                        job.level,
                                        job.maxLevel,
                                        job.experienceOfLevel,
                                        job.maxExperienceOfLevel,
                                        profile.point,
                                        mana,
                                        maxMana
                                    )
                                }
                            }
                        }
                    } else {
                        sender.sendLang("info-no-job", player.name)
                    }
                }
            }
        }
    }

    @CommandBody
    val test = subCommandExec<Player> {
        Bukkit.getScheduler().pendingTasks.forEach {
            info("${it.owner.name} ${it.isSync} ${it.taskId}")
        }
    }

    @CommandBody
    val shutdown = subCommandExec<ConsoleCommandSender> {
        GameManager.shutdownServer()
    }

}
package org.gitee.orryx.command

import com.eatthepath.uuid.FastUUID
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.kether.actions.game.entity.EntityBuilder
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.reload.ReloadAPI
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submitAsync
import taboolib.expansion.createHelper
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang
import java.util.UUID
import kotlin.system.measureTimeMillis

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
                        IManaManager.INSTANCE.getMana(player).thenCompose { mana ->
                            IManaManager.INSTANCE.getMaxMana(player).thenCompose { maxMana ->
                                player.job(profile.id, jobKey) { job ->
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
                        null
                    }
                }
            }
            literal("job") {
                dynamic("job") {
                    suggest { JobLoaderManager.getAllJobLoaders().map { it.key } }
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        player.orryxProfile { profile ->
                            player.job(profile.id, ctx["job"]) { job ->
                                sender.sendLang(
                                    "info-job",
                                    player.name,
                                    job.key,
                                    job.job.name,
                                    job.getAttributes().joinToString(", "),
                                    job.getRegainMana(),
                                    job.getMaxMana(),
                                    job.getUpgradePoint(job.level, job.level + 1),
                                    job.getExperience().key
                                )
                                sendSkills(player, job)
                            }
                        }
                    }
                }
            }
            literal("skills") {
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job { job ->
                        sendSkills(player, job)
                    }
                }
            }
            literal("skill") {
                dynamic("skill") {
                    suggest { SkillLoaderManager.getSkills().map { it.key } }
                    exec<ProxyCommandSender> {
                        val player = ctx.bukkitPlayer() ?: return@exec
                        player.skill(ctx["skill"]) { skill ->
                            sender.sendLang(
                                "info-skill",
                                player.name,
                                skill.key,
                                skill.skill.name,
                                skill.skill.type.uppercase(),
                                skill.skill.isLocked,
                                skill.skill.minLevel,
                                skill.level,
                                skill.skill.maxLevel,
                                skill.parameter().manaValue(),
                                skill.parameter().cooldownValue()/20
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendSkills(player: Player, job: IPlayerJob) {
        var count = job.job.skills.size
        for (i in job.job.skills.indices) {
            player.sendLang("skill", player.name, job.job.skills[i])
            count --
            if (count != 0) {
                player.sendMessage(", ")
            }
        }
    }
}
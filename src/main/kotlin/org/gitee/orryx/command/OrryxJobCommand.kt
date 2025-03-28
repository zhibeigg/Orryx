package org.gitee.orryx.command

import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.*

@CommandHeader("Job", description = "Orryx技能插件职业指令", usage = "Job", permission = "Orryx.Command.Main", permissionMessage = "你没有权限使用此指令")
object OrryxJobCommand {

    @CommandBody
    val change = subCommand {
        player {
            dynamic("job") {
                suggest { JobLoaderManager.getAllJobLoaders().keys.toList() }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job(ctx["job"]) { job ->
                        player.orryxProfile {
                            it.setJob(job)
                            sender.sendMessage("玩家${player.name} 职业已切换到${job.key}")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val clear = subCommand {
        player {
            dynamic("job") {
                suggest { JobLoaderManager.getAllJobLoaders().keys.toList() }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    player.job(ctx["job"]) { job ->
                        job.clear().whenComplete { _, _ ->
                            sender.sendMessage("玩家${player.name} 职业${job.key}已清除")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val experience = OrryxJobExperienceCommand

    @CommandBody
    val level = OrryxJobLevelCommand

}
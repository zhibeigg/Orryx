package org.gitee.orryx.command

import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.core.station.stations.StationLoaderManager
import org.gitee.orryx.utils.bukkitPlayer
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.player
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggest

object OrryxScriptCommand {

    @CommandBody
    val terminateAllSkill = subCommand {
        exec<ProxyCommandSender> {
            ScriptManager.terminateAllSkills()
        }
    }

    @CommandBody
    val terminateAllStation = subCommand {
        exec<ProxyCommandSender> {
            ScriptManager.terminateAllStation()
        }
    }

    @CommandBody
    val terminatePlayerAllSkill = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                ScriptManager.runningSkillScriptsMap[player.uniqueId]?.terminateAll()
            }
        }
    }

    @CommandBody
    val terminatePlayerAllStation = subCommand {
        player {
            exec<ProxyCommandSender> {
                val player = ctx.bukkitPlayer() ?: return@exec
                ScriptManager.runningStationScriptsMap[player.uniqueId]?.terminateAll()
            }
        }
    }

    @CommandBody
    val terminateSkill = subCommand {
        player {
            dynamic("skill") {
                suggest { SkillLoaderManager.getSkills().keys.toList() }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    val skill = ctx["skill"]
                    ScriptManager.runningSkillScriptsMap[player.uniqueId]?.terminate(skill)
                }
            }
        }
    }

    @CommandBody
    val terminateStation = subCommand {
        player {
            dynamic("station") {
                suggest { StationLoaderManager.getStationLoaders().keys.toList() }
                exec<ProxyCommandSender> {
                    val player = ctx.bukkitPlayer() ?: return@exec
                    val station = ctx["station"]
                    ScriptManager.runningStationScriptsMap[player.uniqueId]?.terminate(station)
                }
            }
        }
    }

}
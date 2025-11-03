package org.gitee.orryx.command

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI.Companion.effectScope
import org.gitee.orryx.api.OrryxAPI.Companion.ioScope
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.subCommandExec
import taboolib.common.platform.function.info
import taboolib.common.platform.function.isPrimaryThread
import taboolib.platform.util.sendLang
import java.util.*

object OrryxTestCommand {

    private val unlimits = mutableListOf<UUID>()

    fun isUnlimited(player: Player): Boolean {
        return unlimits.contains(player.uniqueId)
    }

    @CommandBody
    val unlimit: subCommand = subCommand {
        exec<Player> {
            if (unlimits.contains(sender.uniqueId)) {
                unlimits.remove(sender.uniqueId)
                sender.sendLang("out-unlimit-mode")
            } else {
                unlimits.add(sender.uniqueId)
                sender.sendLang("in-unlimit-mode")
            }
        }
    }

    @CommandBody
    val test: subCommandExec = subCommandExec<ProxyCommandSender> {
        var time = System.currentTimeMillis()
        ioScope.launch { info("io: $isPrimaryThread") }.invokeOnCompletion {
            info("io: ${System.currentTimeMillis() - time}ms")

            time = System.currentTimeMillis()
            effectScope.launch { info("effect: $isPrimaryThread") }.invokeOnCompletion {
                info("effect: ${System.currentTimeMillis() - time}ms")

                time = System.currentTimeMillis()
                pluginScope.launch { info("plugin: $isPrimaryThread") }.invokeOnCompletion {
                    info("plugin: ${System.currentTimeMillis() - time}ms")
                }
            }
        }
    }
}
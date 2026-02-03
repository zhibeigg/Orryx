package org.gitee.orryx.command

import org.bukkit.entity.Player
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.platform.util.sendLang
import java.util.*

object OrryxTestCommand {

    private val unlimits = mutableListOf<UUID>()

    fun isUnlimited(player: Player): Boolean {
        return unlimits.contains(player.uniqueId)
    }

    @CommandBody
    val unlimit = subCommand {
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
}
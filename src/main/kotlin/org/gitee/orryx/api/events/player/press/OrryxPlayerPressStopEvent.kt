package org.gitee.orryx.api.events.player.press

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.ISkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerPressStopEvent(val player: Player, val skill: ISkill?, val pressTick: Long, val maxPressTick: Long): BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false
}
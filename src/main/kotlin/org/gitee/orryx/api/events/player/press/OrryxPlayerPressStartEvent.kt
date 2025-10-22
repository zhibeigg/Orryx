package org.gitee.orryx.api.events.player.press

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.ISkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerPressStartEvent(val player: Player, val skill: ISkill?, val pressTick: Long): BukkitProxyEvent() {

    override val allowCancelled: Boolean
        get() = false
}
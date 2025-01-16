package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerPointEvents {

    class Up(val player: Player, val profile: IPlayerProfile, var point: Int): BukkitProxyEvent()

    class Down(val player: Player, val profile: IPlayerProfile, var point: Int): BukkitProxyEvent()

}
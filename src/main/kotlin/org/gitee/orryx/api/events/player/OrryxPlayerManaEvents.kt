package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerManaEvents {

    class Up(val player: Player, val profile: IPlayerProfile, var mana: Double): BukkitProxyEvent()

    class Down(val player: Player, val profile: IPlayerProfile, var mana: Double): BukkitProxyEvent()

}
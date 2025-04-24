package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSpiritEvents {

    class Up(val player: Player, val profile: IPlayerProfile, var spirit: Double): BukkitProxyEvent()
    class Down(val player: Player, val profile: IPlayerProfile, var spirit: Double): BukkitProxyEvent()
    /**
     * 此事件可能为异步，注意检测线程安全问题
     * */
    class Regin(val player: Player, val profile: IPlayerProfile, var reginSpirit: Double): BukkitProxyEvent()
    class Heal(val player: Player, val profile: IPlayerProfile, val healSpirit: Double): BukkitProxyEvent()
}
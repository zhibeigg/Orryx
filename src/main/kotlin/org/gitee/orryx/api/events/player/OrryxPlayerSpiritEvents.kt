package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSpiritEvents {

    class Up {

        class Pre(val player: Player, val profile: IPlayerProfile, var spirit: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val spirit: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Down {

        class Pre(val player: Player, val profile: IPlayerProfile, var spirit: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val spirit: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    /**
     * 此事件可能为异步，注意检测线程安全问题
     * */
    class Regain {

        class Pre(val player: Player, val profile: IPlayerProfile, var regainSpirit: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val regainSpirit: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Heal {

        class Pre(val player: Player, val profile: IPlayerProfile, val healSpirit: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val healSpirit: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }
}

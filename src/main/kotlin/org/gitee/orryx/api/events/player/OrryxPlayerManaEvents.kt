package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerManaEvents {

    class Up {

        class Pre(val player: Player, val profile: IPlayerProfile, var mana: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val mana: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

    class Down {

        class Pre(val player: Player, val profile: IPlayerProfile, var mana: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val mana: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

    class Regin {

        class Pre(val player: Player, val profile: IPlayerProfile, var reginMana: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val reginMana: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

    class Heal {

        class Pre(val player: Player, val profile: IPlayerProfile, var healMana: Double): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val healMana: Double): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

}
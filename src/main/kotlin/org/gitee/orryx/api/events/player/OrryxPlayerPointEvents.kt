package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerPointEvents {

    class Up {

        class Pre(val player: Player, val profile: IPlayerProfile, var point: Int): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val point: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

    class Down {

        class Pre(val player: Player, val profile: IPlayerProfile, var point: Int): BukkitProxyEvent()

        class Post(val player: Player, val profile: IPlayerProfile, val point: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

}
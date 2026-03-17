package org.gitee.orryx.api.events.player.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobLevelEvents {

    class Up {

        class Pre(val player: Player, val job: IPlayerJob, var upLevel: Int): BukkitProxyEvent()

        class Post(val player: Player, val job: IPlayerJob, val upLevel: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Down {

        class Pre(val player: Player, val job: IPlayerJob, var downLevel: Int): BukkitProxyEvent()

        class Post(val player: Player, val job: IPlayerJob, val downLevel: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }
}

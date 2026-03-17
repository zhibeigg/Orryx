package org.gitee.orryx.api.events.player.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobExperienceEvents {

    class Up {

        class Pre(val player: Player, val job: IPlayerJob, var upExperience: Int): BukkitProxyEvent()

        class Post(val player: Player, val job: IPlayerJob, val upExperience: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Down {

        class Pre(val player: Player, val job: IPlayerJob, var downExperience: Int): BukkitProxyEvent()

        class Post(val player: Player, val job: IPlayerJob, val downExperience: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }
    }
}

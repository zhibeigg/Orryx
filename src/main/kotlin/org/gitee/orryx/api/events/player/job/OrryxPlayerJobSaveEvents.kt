package org.gitee.orryx.api.events.player.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobSaveEvents {

    class Pre(
        val player: Player,
        val job: IPlayerJob,
        var async: Boolean,
        var remove: Boolean
    ): BukkitProxyEvent()

    class Post(
        val player: Player,
        val job: IPlayerJob,
        val async: Boolean,
        val remove: Boolean
    ): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}

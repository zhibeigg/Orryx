package org.gitee.orryx.api.events.player.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobClearEvents {

    class Pre(val player: Player, val job: IPlayerJob): BukkitProxyEvent()

    class Post(val player: Player, val job: IPlayerJob): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}
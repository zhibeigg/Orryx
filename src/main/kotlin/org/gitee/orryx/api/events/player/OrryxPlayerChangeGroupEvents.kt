package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.key.IGroup
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerChangeGroupEvents {

    class Pre(val player: Player, val job: IPlayerJob, var group: IGroup): BukkitProxyEvent()

    class Post(val player: Player, val job: IPlayerJob, val group: IGroup): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}
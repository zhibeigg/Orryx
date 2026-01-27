package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerProfileSaveEvents {

    class Pre(
        val player: Player,
        val profile: IPlayerProfile,
        var async: Boolean,
        var remove: Boolean
    ): BukkitProxyEvent()

    class Post(
        val player: Player,
        val profile: IPlayerProfile,
        val async: Boolean,
        val remove: Boolean
    ): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}

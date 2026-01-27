package org.gitee.orryx.api.events.player.key

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerKeySettingSaveEvents {

    class Pre(
        val player: Player,
        val setting: PlayerKeySetting,
        var async: Boolean,
        var remove: Boolean
    ): BukkitProxyEvent()

    class Post(
        val player: Player,
        val setting: PlayerKeySetting,
        val async: Boolean,
        val remove: Boolean
    ): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}

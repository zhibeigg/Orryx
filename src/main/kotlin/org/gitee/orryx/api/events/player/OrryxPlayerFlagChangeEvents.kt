package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.IPlayerProfile
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerFlagChangeEvents {

    class Pre(val player: Player, val profile: IPlayerProfile, var flagName: String, val oldFlag: IFlag?, var newFlag: IFlag?): BukkitProxyEvent()

    class Post(val player: Player, val profile: IPlayerProfile, val flagName: String, val oldFlag: IFlag?, val newFlag: IFlag?): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}
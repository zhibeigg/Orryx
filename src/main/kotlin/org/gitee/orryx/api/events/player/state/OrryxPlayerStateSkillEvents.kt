package org.gitee.orryx.api.events.player.state

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.module.state.states.SkillState
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerStateSkillEvents {

    class Pre(val player: Player, val skillParameter: IParameter, var silence: Long, val state: SkillState.Running): BukkitProxyEvent()

    class Post(val player: Player, val skillParameter: IParameter, val silence: Long, val state: SkillState.Running): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}
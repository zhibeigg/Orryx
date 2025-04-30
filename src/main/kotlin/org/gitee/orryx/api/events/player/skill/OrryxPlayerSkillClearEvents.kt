package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillClearEvents {

    class Pre(val player: Player, val skill: IPlayerSkill): BukkitProxyEvent()

    class Post(val player: Player, val skill: IPlayerSkill): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}
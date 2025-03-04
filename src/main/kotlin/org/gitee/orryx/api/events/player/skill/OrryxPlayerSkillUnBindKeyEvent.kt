package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillUnBindKeyEvent {

    class Pre(val player: Player, val skill: IPlayerSkill, var group: IGroup): BukkitProxyEvent()

    class Post(val player: Player, val skill: IPlayerSkill, val group: IGroup): BukkitProxyEvent() {
        override val allowCancelled: Boolean
            get() = false
    }

}
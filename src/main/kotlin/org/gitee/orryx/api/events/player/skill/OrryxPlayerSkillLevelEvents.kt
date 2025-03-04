package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillLevelEvents {

    class Up {

        class Pre(val player: Player, val skill: IPlayerSkill, var upLevel: Int): BukkitProxyEvent()

        class Post(val player: Player, val skill: IPlayerSkill, val upLevel: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

    class Down {

        class Pre(val player: Player, val skill: IPlayerSkill, var downLevel: Int): BukkitProxyEvent()

        class Post(val player: Player, val skill: IPlayerSkill, val downLevel: Int): BukkitProxyEvent() {
            override val allowCancelled: Boolean
                get() = false
        }

    }

}
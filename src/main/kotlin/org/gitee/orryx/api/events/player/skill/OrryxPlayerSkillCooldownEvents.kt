package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillCooldownEvents {

    class Set {

        class Pre(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

        class Post(val player: Player, val skill: IPlayerSkill, val amount: Long): BukkitProxyEvent() {

            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Increase {

        class Pre(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

        class Post(val player: Player, val skill: IPlayerSkill, val amount: Long): BukkitProxyEvent() {

            override val allowCancelled: Boolean
                get() = false
        }
    }

    class Reduce {

        class Pre(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

        class Post(val player: Player, val skill: IPlayerSkill, val amount: Long): BukkitProxyEvent() {

            override val allowCancelled: Boolean
                get() = false
        }
    }
}
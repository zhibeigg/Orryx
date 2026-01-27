package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillSaveEvents {

    class Pre(
        val player: Player,
        val skill: IPlayerSkill,
        var async: Boolean,
        var remove: Boolean
    ): BukkitProxyEvent()

    class Post(
        val player: Player,
        val skill: IPlayerSkill,
        val async: Boolean,
        val remove: Boolean
    ): BukkitProxyEvent() {

        override val allowCancelled: Boolean
            get() = false
    }
}

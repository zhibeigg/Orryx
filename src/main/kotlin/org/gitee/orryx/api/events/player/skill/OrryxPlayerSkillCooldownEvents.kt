package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillCooldownEvents {

    class Set(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

    class Increase(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

    class Reduce(val player: Player, val skill: IPlayerSkill, var amount: Long): BukkitProxyEvent()

}
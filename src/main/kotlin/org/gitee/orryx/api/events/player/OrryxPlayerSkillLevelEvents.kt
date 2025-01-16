package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillLevelEvents {

    class Up(val player: Player, val skill: IPlayerSkill, var upLevel: Int): BukkitProxyEvent()

    class Down(val player: Player, val skill: IPlayerSkill, var downLevel: Int): BukkitProxyEvent()

}
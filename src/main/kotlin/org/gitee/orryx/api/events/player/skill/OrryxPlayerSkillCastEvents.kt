package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillCastEvents {

    class Check(val player: Player, val skill: IPlayerSkill, val skillParameter: IParameter): BukkitProxyEvent()

    class Cast(val player: Player, val skill: IPlayerSkill, val skillParameter: IParameter): BukkitProxyEvent()
}
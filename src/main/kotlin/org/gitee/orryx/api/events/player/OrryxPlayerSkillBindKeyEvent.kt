package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerSkillBindKeyEvent(val player: Player, val skill: IPlayerSkill, var group: IGroup, var bindKey: IBindKey): BukkitProxyEvent()
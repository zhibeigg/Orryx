package org.gitee.orryx.api.events.player.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxClearAllSkillLevelAndBackPointEvent(val player: Player, val job: IPlayerJob): BukkitProxyEvent()
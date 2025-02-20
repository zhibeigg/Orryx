package org.gitee.orryx.api.events.player.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobChangeEvent(val player: Player, val job: IPlayerJob): BukkitProxyEvent()
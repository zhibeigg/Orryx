package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobLevelEvents {

    class Up(val player: Player, val job: IPlayerJob, val upLevel: Int): BukkitProxyEvent()

    class Down(val player: Player, val job: IPlayerJob, val downLevel: Int): BukkitProxyEvent()

}
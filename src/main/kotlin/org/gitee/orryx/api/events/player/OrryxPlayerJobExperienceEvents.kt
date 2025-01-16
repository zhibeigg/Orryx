package org.gitee.orryx.api.events.player

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import taboolib.platform.type.BukkitProxyEvent

class OrryxPlayerJobExperienceEvents {

    class Up(val player: Player, val job: IPlayerJob, var upExperience: Int): BukkitProxyEvent()

    class Down(val player: Player, val job: IPlayerJob, var downExperience: Int): BukkitProxyEvent()

}
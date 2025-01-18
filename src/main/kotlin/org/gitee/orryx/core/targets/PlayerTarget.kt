package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.AbstractEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity

class PlayerTarget(val player: Player): ITargetEntity<Player> {

    override val entity: AbstractEntity
        get() = AbstractBukkitEntity(player)

    override val location: Location
        get() = entity.location

    override val eyeLocation: Location
        get() = entity.eyeLocation

    override val world: World
        get() = entity.world

    val uniqueId
        get() = player.uniqueId

    override fun getSource(): Player {
        return player
    }

    override fun toString(): String {
        return player.toString()
    }

}
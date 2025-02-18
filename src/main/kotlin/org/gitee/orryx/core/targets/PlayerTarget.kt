package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity

class PlayerTarget(private val player: Player) : ITargetEntity<Player>, ITargetLocation<Player>, IEntity by AbstractBukkitEntity(player) {

    override val entity: IEntity
        get() = this

    override val location: Location
        get() = player.location

    override val eyeLocation: Location
        get() = player.eyeLocation

    override val world: World
        get() = player.world

    override val uniqueId by lazy {
        player.uniqueId
    }

    override fun getSource(): Player {
        return player
    }

    override fun toString(): String {
        return "PlayerTarget(name=${player.name}, uuid=${player.uniqueId})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlayerTarget
        return player.uniqueId == other.player.uniqueId
    }

    override fun hashCode(): Int {
        return player.uniqueId.hashCode()
    }

}
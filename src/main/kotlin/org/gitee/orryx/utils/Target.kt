package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.targets.PlayerTarget

internal fun Player.toTarget(): ITarget<Player> {
    return PlayerTarget(this)
}

internal fun Entity.toTarget(): ITarget<Entity> {
    return AbstractBukkitEntity(this)
}

internal fun EntityInstance.toTarget(): ITarget<EntityInstance> {
    return AbstractAdyeshachEntity(this)
}

internal fun Location.toTarget(): ITarget<Location> {
    return LocationTarget(this)
}
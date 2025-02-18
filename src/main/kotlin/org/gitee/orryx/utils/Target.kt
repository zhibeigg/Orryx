package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.gitee.orryx.api.adapters.entity.AbstractAdyeshachEntity
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.targets.PlayerTarget

internal fun Player.toTarget(): PlayerTarget {
    return PlayerTarget(this)
}

internal fun Entity.toTarget(): ITargetEntity<*> {
    if (this is Player) {
        return PlayerTarget(this)
    }
    return AbstractBukkitEntity(this)
}

internal fun EntityInstance.toTarget(): AbstractAdyeshachEntity {
    return AbstractAdyeshachEntity(this)
}

internal fun Location.toTarget(): LocationTarget {
    return LocationTarget(this)
}
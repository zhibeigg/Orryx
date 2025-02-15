package org.gitee.orryx.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector
import org.joml.Vector3dc
import taboolib.common5.Coerce

internal fun Any.toLocation(world: World? = null): Location {
    return when (this) {
        is Location -> this.also { it.world = world }
        is String -> {
            val split = split(",")
            Location(
                world ?: Bukkit.getWorld(split[0]),
                Coerce.toDouble(split[1]),
                Coerce.toDouble(split[2]),
                Coerce.toDouble(split[3])
            )
        }
        is Vector3dc -> Location(world, x(), y(), z())
        is Vector -> Location(world, x, y, z)
        else -> Location(world, 0.0, 0.0, 0.0)
    }
}
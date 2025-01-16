package org.gitee.orryx.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import taboolib.common5.Coerce

internal fun Any.toLocation(): Location {
    return when (this) {
        is Location -> this
        is String -> {
            val split = split(",")
            Location(
                Bukkit.getWorld(split[0]),
                Coerce.toDouble(split[1]),
                Coerce.toDouble(split[2]),
                Coerce.toDouble(split[3])
            )
        }
        else -> Location(null, 0.0, 0.0, 0.0)
    }
}
package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World

class LocationTarget(override val location: Location): ITargetLocation<Location> {

    override val world: World
        get() = location.world!!

    override val eyeLocation = location

    override fun getSource(): Location {
        return location
    }

    override fun toString(): String {
        return location.toString()
    }

}
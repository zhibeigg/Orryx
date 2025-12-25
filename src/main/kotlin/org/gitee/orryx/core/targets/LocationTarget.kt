package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World
import org.gitee.orryx.api.adapters.vector.AbstractVector

class LocationTarget(override val location: Location): ITargetLocation<Location>, AbstractVector(location) {

    override val world: World
        get() = location.world!!

    override val eyeLocation: Location = location

    override fun getSource(): Location {
        return location
    }

    override fun toString(): String {
        return location.toString()
    }

}
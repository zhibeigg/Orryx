package org.gitee.orryx.core.targets

import org.bukkit.Location
import org.bukkit.World

interface ITargetLocation<T>: ITarget<T> {

    val world: World

    val location: Location

    val eyeLocation: Location

}
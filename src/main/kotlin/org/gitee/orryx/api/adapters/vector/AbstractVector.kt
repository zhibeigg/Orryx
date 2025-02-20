package org.gitee.orryx.api.adapters.vector

import org.bukkit.Location
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.utils.joml
import org.joml.Vector3d
import org.joml.Vector3dc

open class AbstractVector(override val joml: Vector3d): IVector, Vector3dc by joml {

    constructor(vector: Vector): this(vector.joml())

    constructor(location: Location): this(location.joml())

    constructor(): this(Vector3d())

    override fun add(vector3dc: Vector3dc): AbstractVector {
        joml.add(vector3dc)
        return this
    }

    override fun negate(): AbstractVector {
        joml.negate()
        return this
    }

    override fun toString(): String {
        return joml.toString()
    }

}
package org.gitee.orryx.api.adapters.vector

import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IVector
import org.joml.Vector3d
import org.joml.Vector3dc

open class AbstractVector(override val joml: Vector3d): IVector, Vector3dc by joml {

    override fun getBukkit(): Vector {
        return Vector(joml.x, joml.y, joml.z)
    }

    override fun negate(): AbstractVector {
        joml.negate()
        return this
    }

}
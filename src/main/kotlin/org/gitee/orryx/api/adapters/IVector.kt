package org.gitee.orryx.api.adapters

import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.joml.Vector3d
import org.joml.Vector3dc

interface IVector: Vector3dc {

    val joml: Vector3d

    fun add(vector3dc: Vector3dc): AbstractVector

    fun negate(): IVector

}
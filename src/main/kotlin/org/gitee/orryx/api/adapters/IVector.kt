package org.gitee.orryx.api.adapters

import org.joml.Vector3d

interface IVector {

    val joml: Vector3d

    fun negate(): IVector

}
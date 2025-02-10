package org.gitee.orryx.api.adapters

import org.bukkit.util.Vector
import org.joml.Vector3d

interface IVector {

    val joml: Vector3d

    fun getBukkit(): Vector

    fun negate(): IVector

}
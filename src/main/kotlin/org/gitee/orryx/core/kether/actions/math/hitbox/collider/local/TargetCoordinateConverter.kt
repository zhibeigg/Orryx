package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * Target 坐标转换器
 * */
class TargetCoordinateConverter(private val target: ITargetLocation<*>) : ICoordinateConverter {

    private val version = ShortArray(2)

    override val position: Vector3d = Vector3d()

    val yRot: Double
        get() = target.location.yaw.toDouble()

    override val rotation: Quaterniond = Quaterniond()

    override fun positionVersion(): Short {
        return version[0]
    }

    override fun rotationVersion(): Short {
        return version[1]
    }

    fun update() {
        val pos = target.location

        if (!position.equals(pos.x, pos.y, pos.z)) {
            position.set(pos.x, pos.y, pos.z)
            version[0]++
        }
    }
}
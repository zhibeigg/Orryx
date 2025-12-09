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

    override val position: Vector3d = Vector3d(target.location.x, target.location.y, target.location.z)

    private var yRot: Double = target.location.yaw.toDouble()

    override val rotation: Quaterniond = Quaterniond().setAngleAxis(Math.toDegrees(yRot), 0.0, 1.0, 0.0)

    override fun positionVersion(): Short {
        return version[0]
    }

    override fun rotationVersion(): Short {
        return version[1]
    }

    override fun update() {
        val pos = target.location

        if (!position.equals(pos.x, pos.y, pos.z)) {
            position.set(pos.x, pos.y, pos.z)
            version[0]++
        }

        if (yRot != target.location.yaw.toDouble()) {
            yRot = target.location.yaw.toDouble()
            rotation.setAngleAxis(Math.toDegrees(yRot), 0.0, 1.0, 0.0)
            version[1]++
        }
    }
}
package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.adapters.IEntity
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.joml.Quaterniond
import org.joml.Vector3d

/** Entity坐标转换器 */
class EntityCoordinateConverter(private val entity: IEntity) : ICoordinateConverter {

    private val version = ShortArray(2)

    override val position: Vector3d = Vector3d()

    private val yRot = 0.0

    override val rotation: Quaterniond = Quaterniond()

    override fun positionVersion(): Short {
        return version[0]
    }

    override fun rotationVersion(): Short {
        return version[1]
    }

    fun update() {
        val pos = entity.location

        if (!position.equals(pos.x, pos.y, pos.z)) {
            position.set(pos.x, pos.y, pos.z)
            version[0]++
        }
    }
}
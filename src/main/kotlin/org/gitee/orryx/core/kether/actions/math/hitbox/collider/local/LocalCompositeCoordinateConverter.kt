package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.joml.Quaterniond
import org.joml.Vector3d

class LocalCompositeCoordinateConverter(private val composite: LocalComposite<*, *>) : ICoordinateConverter {

    private val version = ShortArray(2)

    override fun positionVersion(): Short {
        return version[0]
    }

    override val position: Vector3d
        get() = composite.position

    override fun rotationVersion(): Short {
        return version[1]
    }

    override val rotation: Quaterniond
        get() = composite.rotation

    fun update() {
        version[0]++
        version[1]++
    }
}
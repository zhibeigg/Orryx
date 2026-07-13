package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.joml.Quaterniond
import org.joml.Vector3d

class LocalCompositeCoordinateConverter(private val composite: LocalComposite<*, *>) : ICoordinateConverter {

    private val version = ShortArray(2)
    private val cachedPosition = Vector3d(Double.NaN, Double.NaN, Double.NaN)
    private val cachedRotation = Quaterniond(Double.NaN, Double.NaN, Double.NaN, Double.NaN)

    override fun positionVersion(): Short {
        return version[0]
    }

    override val position: Vector3d
        get() = cachedPosition

    override fun rotationVersion(): Short {
        return version[1]
    }

    override val rotation: Quaterniond
        get() = cachedRotation

    override fun update() {
        if (composite.disable()) return

        composite.update()
        val latestRotation = composite.rotation
        val latestPosition = composite.position
        if (cachedPosition != latestPosition) {
            cachedPosition.set(latestPosition)
            version[0]++
        }
        if (cachedRotation != latestRotation) {
            cachedRotation.set(latestRotation)
            version[1]++
        }
    }

}
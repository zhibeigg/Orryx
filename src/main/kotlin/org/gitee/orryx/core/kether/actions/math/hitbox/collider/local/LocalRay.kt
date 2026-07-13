package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalRay
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic.AABBPlus
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs

open class LocalRay<T : ITargetLocation<*>>(
    localOrigin: Vector3d,
    localDirection: Vector3d,
    length: Double,
    private val parent: ICoordinateConverter
) : ILocalRay<T> {

    private val globalOrigin = Vector3d()
    private val globalDirection = Vector3d()
    private val globalEnd = Vector3d()
    private val fastCenter = Vector3d()
    private val fastExtents = Vector3d()
    private val fastBounds = AABBPlus<T>(fastExtents, fastCenter)
    private val version = ShortArray(2)
    private var dirty = true
    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var length: Double = length
        set(value) { dirty = true; field = value }
    override var localOrigin = localOrigin
        set(value) { dirty = true; field = value }
    override var localDirection = localDirection
        set(value) { dirty = true; field = value }

    override val origin: Vector3d get() { update(); return globalOrigin }
    override val end: Vector3d get() { update(); return globalEnd }

    override var direction: Vector3d
        get() { update(); return globalDirection }
        set(direction) {
            dirty = true
            version[1] = parent.rotationVersion()
            val rotation: Quaterniond = parent.rotation.conjugate(Quaterniond())
            localDirection.set(direction).rotate(rotation)
            globalDirection.set(direction)
        }

    override fun setDisable(disable: Boolean) { this.disable = disable }
    override fun disable(): Boolean = disable
    protected fun setOriginOrDirectionDirty() { dirty = true }

    override fun update() {
        parent.update()
        if (parent.positionVersion() == version[0] && parent.rotationVersion() == version[1] && !dirty) return
        dirty = false
        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
        parent.rotation.transform(localOrigin, globalOrigin).add(parent.position)
        parent.rotation.transform(localDirection, globalDirection)
        globalEnd.set(globalDirection).mul(length).add(globalOrigin)
        fastCenter.set(globalOrigin).add(globalEnd).mul(0.5)
        fastExtents.set(
            abs(globalEnd.x - globalOrigin.x) / 2.0,
            abs(globalEnd.y - globalOrigin.y) / 2.0,
            abs(globalEnd.z - globalOrigin.z) / 2.0
        )
    }

    override val fastCollider: IAABB<T>
        get() { update(); return fastBounds }
}

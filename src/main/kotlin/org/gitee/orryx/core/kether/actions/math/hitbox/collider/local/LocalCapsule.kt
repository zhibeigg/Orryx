package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalCapsule
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic.AABBPlus
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs

open class LocalCapsule<T : ITargetLocation<*>>(
    height: Double,
    radius: Double,
    localCenter: Vector3d,
    localRotation: Quaterniond,
    private val parent: ICoordinateConverter
) : ILocalCapsule<T> {

    private val globalCenter = Vector3d()
    private val globalRotation = Quaterniond()
    private val globalDirection = Vector3d()
    private val fastExtents = Vector3d()
    private val fastBounds = AABBPlus<T>(fastExtents, globalCenter)
    private val version = ShortArray(2)
    private val dirty = booleanArrayOf(true, true, true)
    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var height: Double = height
        set(value) { dirty[2] = true; field = value }
    override var radius: Double = radius
        set(value) { dirty[2] = true; field = value }
    override var localCenter = localCenter
        set(value) { dirty[0] = true; field = value }
    override var localRotation = localRotation
        set(value) { dirty[1] = true; field = value }

    override var center: Vector3d
        get() { update(); return globalCenter }
        set(center) {
            dirty[0] = true
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            localCenter.set(center).sub(parent.position).rotate(parent.rotation.conjugate(Quaterniond()))
            globalCenter.set(center)
        }

    override var rotation: Quaterniond
        get() { update(); return globalRotation }
        set(rotation) {
            dirty[1] = true
            version[1] = parent.rotationVersion()
            localRotation.set(rotation).mul(parent.rotation.conjugate(Quaterniond()))
            globalRotation.set(rotation)
        }

    override val direction: Vector3d get() { update(); return globalDirection }
    override fun setDisable(disable: Boolean) { this.disable = disable }
    override fun disable(): Boolean = disable
    protected fun setCenterDirty() { dirty[0] = true }
    protected fun setRotationDirty() { dirty[1] = true }

    override fun update() {
        parent.update()
        val positionChanged = parent.positionVersion() != version[0]
        val rotationChanged = parent.rotationVersion() != version[1]
        if (!positionChanged && !rotationChanged && dirty.none { it }) return

        if (rotationChanged || dirty[1]) {
            parent.rotation.mul(localRotation, globalRotation)
            globalDirection.set(0.0, 1.0, 0.0).rotate(globalRotation)
        }
        if (positionChanged || rotationChanged || dirty[0]) {
            parent.rotation.transform(localCenter, globalCenter).add(parent.position)
        }
        if (rotationChanged || dirty[1] || dirty[2]) {
            val halfHeight = height / 2.0
            fastExtents.set(
                abs(globalDirection.x) * halfHeight + radius,
                abs(globalDirection.y) * halfHeight + radius,
                abs(globalDirection.z) * halfHeight + radius
            )
        }
        dirty.fill(false)
        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
    }

    override val fastCollider: IAABB<T>
        get() { update(); return fastBounds }
}

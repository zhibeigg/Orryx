package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalCapsule
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

open class LocalCapsule<T : ITargetLocation<*>>(
    override var height: Double,
    override var radius: Double,
    localCenter: Vector3d,
    localRotation: Quaterniond,
    private val parent: ICoordinateConverter,
    override val onCollideFunction: ICollideFunction
) : ILocalCapsule<T> {

    private val globalCenter = Vector3d()
    private val globalRotation = Quaterniond()
    private val globalDirection = Vector3d()

    /** 0 - 中心点, 1 - 旋转 */
    private val version = ShortArray(2)

    /** 0 - 中心点, 1 - 旋转 */
    private val dirty = booleanArrayOf(true, true)
    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var localCenter = localCenter
        set(value) {
            dirty[0] = true
            field = value
        }

    override var localRotation = localRotation
        set(value) {
            dirty[1] = true
            field = value
        }

    override var center: Vector3d
        get() {
            update()
            return globalCenter
        }
        set(center) {
            dirty[0] = true
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            val position: Vector3d? = parent.position
            val rotation: Quaterniond? = parent.rotation.conjugate(Quaterniond())

            localCenter.set(center).sub(position).rotate(rotation)
            globalCenter.set(center)
        }

    override var rotation: Quaterniond
        get() {
            update()
            return globalRotation
        }
        set(rotation) {
            dirty[1] = true
            version[1] = parent.rotationVersion()
            localRotation.set(rotation).mul(parent.rotation.conjugate(Quaterniond()))
            globalRotation.set(rotation)
        }

    override val direction: Vector3d
        get() {
            update()
            return globalDirection
        }

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    protected fun setCenterDirty() {
        dirty[0] = true
    }

    protected fun setRotationDirty() {
        dirty[1] = true
    }

    private fun update() {
        if (parent.rotationVersion() != version[1] || dirty[1]) {
            dirty[1] = false
            val rotation = parent.rotation
            rotation.mul(localRotation, globalRotation)
            globalDirection.set(0.0, 1.0, 0.0).rotate(globalRotation)
            version[1] = parent.rotationVersion()
        }

        if (parent.positionVersion() != version[0] || parent.rotationVersion() != version[1] || dirty[0]) {
            dirty[0] = false
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            val position = parent.position
            val rotation = parent.rotation
            rotation.transform(localCenter, globalCenter).add(position)
        }
    }
}
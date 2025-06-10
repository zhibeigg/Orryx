package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalAABB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

open class LocalAABB<T : ITargetLocation<*>>(
    localCenter: Vector3d,
    override var halfExtents: Vector3d,
    private val parent: ICoordinateConverter
) : ILocalAABB<T> {
    
    private val globalCenter: Vector3d = Vector3d()

    /** 0 - 中心点, 1 - 旋转 */
    private val version = ShortArray(2)

    /** 中心点 */
    private var dirty = true
    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }
    override var localCenter: Vector3d = localCenter
        set(value) {
            dirty = true
            field = value
        }
    
    override val localMin: Vector3d
        get() = localCenter.sub(halfExtents, Vector3d())

    override val localMax: Vector3d
        get() = localCenter.add(halfExtents, Vector3d())

    override var center: Vector3d
        get() {
            update()
            return globalCenter
        }
        set(center) {
            dirty = true
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            val position: Vector3d = parent.position
            val rotation: Quaterniond = parent.rotation.conjugate(Quaterniond())

            localCenter.set(center).sub(position).rotate(rotation)
            globalCenter.set(center)
        }

    override val min: Vector3d
        get() = this.center.sub(halfExtents, Vector3d())

    override val max: Vector3d
        get() = this.center.add(halfExtents, Vector3d())

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    protected fun setCenterDirty() {
        dirty = true
    }

    override fun update() {
        if (parent.positionVersion() == version[0] && parent.rotationVersion() == version[1] && !dirty) {
            return
        }

        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
        dirty = false
        val position = parent.position
        val rotation = parent.rotation
        rotation.transform(localCenter, globalCenter).add(position)
    }
}
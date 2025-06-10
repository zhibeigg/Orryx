package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalRay
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

open class LocalRay<T : ITargetLocation<*>>(
    localOrigin: Vector3d, // 射线的起点
    localDirection: Vector3d, // 射线的长度
    override var length: Double,
    private val parent: ICoordinateConverter
) : ILocalRay<T> {

    private val globalOrigin = Vector3d() // 射线的起点
    private val globalDirection = Vector3d()

    /** 0 - 中心点, 1 - 旋转 */
    private val version = ShortArray(2)
    private var dirty = true

    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var localOrigin = localOrigin
        set(value) {
            dirty = true
            field = value
        }

    override var localDirection = localDirection
        set(value) {
            dirty = true
            field = value
        }

    override val origin: Vector3d
        get() {
            update()
            return globalOrigin
        }

    override val end: Vector3d
        get() {
            update()
            return globalDirection.mul(length, Vector3d()).add(globalOrigin)
        }

    override var direction: Vector3d
        get() {
            update()
            return globalDirection
        }
        set(direction) {
            dirty = true
            version[1] = parent.rotationVersion()
            val rotation: Quaterniond? = parent.rotation.conjugate(Quaterniond())
            localDirection.set(direction).rotate(rotation)
            globalDirection.set(direction)
        }

    override fun setDisable(disable: Boolean) {
        dirty = true
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    protected fun setOriginOrDirectionDirty() {
        dirty = true
    }

    override fun update() {
        if (parent.positionVersion() == version[0] && parent.rotationVersion() == version[1] && !dirty) {
            return
        }

        dirty = false
        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
        val position = parent.position
        val rotation = parent.rotation
        rotation.transform(this.localOrigin, globalOrigin).add(position)
        rotation.transform(this.localDirection, globalDirection)
    }
}
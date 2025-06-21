package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import eos.moe.armourers.r
import ink.ptms.adyeshach.module.editor.page.Vector3
import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalSphere
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

open class LocalSphere<T : ITargetLocation<*>>(
    localCenter: Vector3d,
    override var radius: Double,
    private val parent: ICoordinateConverter
) : ILocalSphere<T> {

    private val globalCenter = Vector3d()
    private val version = ShortArray(2)
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

    override var center: Vector3d
        get() {
            update()
            return globalCenter
        }
        set(center) {
            dirty = true
            version[0] = parent.positionVersion()
            version[1] = parent.rotationVersion()
            val position: Vector3d? = parent.position
            val rotation: Quaterniond? = parent.rotation.conjugate(Quaterniond())

            localCenter.set(center).sub(position).rotate(rotation)
            globalCenter.set(center)
        }

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
        parent.update()

        if (parent.positionVersion() == version[0] && parent.rotationVersion() == version[1] && !dirty) {
            return
        }

        dirty = false
        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
        val position = parent.position
        val rotation = parent.rotation
        rotation.transform(this.localCenter, globalCenter).add(position)
    }

    override val fastCollider: IAABB<T>?
        get() = LocalAABB(
            localCenter,
            Vector3d(radius, radius, radius),
            parent
        )
}
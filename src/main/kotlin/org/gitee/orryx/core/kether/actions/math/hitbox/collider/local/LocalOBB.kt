package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalOBB
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.bukkit
import org.joml.Quaterniond
import org.joml.Vector3d
import taboolib.platform.util.onlinePlayers
import kotlin.collections.get

open class LocalOBB<T : ITargetLocation<*>>(
    halfExtents: Vector3d,
    localCenter: Vector3d,
    localRotation: Quaterniond,
    val parent: ICoordinateConverter
) : ILocalOBB<T> {

    private val globalCenter = Vector3d()
    private val globalRotation = Quaterniond()

    override val vertices = Array(8) { Vector3d() }
    override val axes = Array(3) { Vector3d() }

    /** 0 - 中心点, 1 - 旋转 */
    private val version = ShortArray(2)

    /** 0 - 中心点, 1 - 旋转, 2 - 轴半长 */
    private val dirty = booleanArrayOf(true, true, true)
    private var disable = false

    init {
        version[0] = (parent.positionVersion() - 1).toShort()
        version[1] = (parent.rotationVersion() - 1).toShort()
    }

    override var localCenter: Vector3d = localCenter
        set(value) {
            dirty[0] = true
            field = value
        }

    override var localRotation: Quaterniond = localRotation
        set(value) {
            dirty[1] = true
            field = value
        }

    override var halfExtents: Vector3d = halfExtents
        set(value) {
            dirty[2] = true
            field = value
        }

    override var center: Vector3d
        get() = globalCenter
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
        get() = globalRotation
        set(rotation) {
            dirty[1] = true
            version[1] = parent.rotationVersion()
            localRotation.set(rotation).mul(parent.rotation.conjugate(Quaterniond()))
            globalRotation.set(rotation)
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

    protected fun setHalfExtentsDirty() {
        dirty[2] = true
    }

    override fun update() {
        parent.update()

        if ((!dirty[1] && !dirty[2] && (version[0] != parent.positionVersion() && version[1] == parent.rotationVersion()) || dirty[0])) {
            dirty[0] = false
            version[0] = parent.positionVersion()
            //仅进行位置移动
            val center = parent.rotation.transform(this.localCenter, Vector3d()).add(parent.position)
            val offset = center.sub(globalCenter, Vector3d())
            globalCenter.set(center)

            for (vertex in vertices) {
                vertex.add(offset)
            }

            return
        } else if (version[0] == parent.positionVersion() && version[1] == parent.rotationVersion() && !dirty[0] && !dirty[1] && !dirty[2]) {
            return
        }

        dirty[0] = false
        dirty[1] = false
        dirty[2] = false
        version[0] = parent.positionVersion()
        version[1] = parent.rotationVersion()
        val position = parent.position
        val rotation = parent.rotation
        val center = rotation.transform(this.localCenter, globalCenter).add(position)
        rotation.mul(this.localRotation, globalRotation)

        // 旋转轴向
        axes[0] = globalRotation.transform(Vector3d(1.0, 0.0, 0.0))
        axes[1] = globalRotation.transform(Vector3d(0.0, 1.0, 0.0))
        axes[2] = globalRotation.transform(Vector3d(0.0, 0.0, 1.0))

        val v = Vector3d()

        // 计算顶点
        vertices[0] = Vector3d().set(center).add(axes[0].mul(halfExtents.x, v)).add(axes[1].mul(halfExtents.y, v)).add(axes[2].mul(halfExtents.z, v))
        vertices[1] = Vector3d().set(center).add(axes[0].mul(halfExtents.x, v)).add(axes[1].mul(halfExtents.y, v)).sub(axes[2].mul(halfExtents.z, v))
        vertices[2] = Vector3d().set(center).add(axes[0].mul(halfExtents.x, v)).sub(axes[1].mul(halfExtents.y, v)).add(axes[2].mul(halfExtents.z, v))
        vertices[3] = Vector3d().set(center).add(axes[0].mul(halfExtents.x, v)).sub(axes[1].mul(halfExtents.y, v)).sub(axes[2].mul(halfExtents.z, v))
        vertices[4] = Vector3d().set(center).sub(axes[0].mul(halfExtents.x, v)).add(axes[1].mul(halfExtents.y, v)).add(axes[2].mul(halfExtents.z, v))
        vertices[5] = Vector3d().set(center).sub(axes[0].mul(halfExtents.x, v)).add(axes[1].mul(halfExtents.y, v)).sub(axes[2].mul(halfExtents.z, v))
        vertices[6] = Vector3d().set(center).sub(axes[0].mul(halfExtents.x, v)).sub(axes[1].mul(halfExtents.y, v)).add(axes[2].mul(halfExtents.z, v))
        vertices[7] = Vector3d().set(center).sub(axes[0].mul(halfExtents.x, v)).sub(axes[1].mul(halfExtents.y, v)).sub(axes[2].mul(halfExtents.z, v))
    }

    override val fastCollider: IAABB<T>?
        get() {
            val length = halfExtents.length()
            return LocalAABB(
                localCenter,
                Vector3d(length, length, length),
                parent
            )
        }
}
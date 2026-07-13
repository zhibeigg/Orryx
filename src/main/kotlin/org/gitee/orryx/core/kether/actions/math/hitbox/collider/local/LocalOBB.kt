package org.gitee.orryx.core.kether.actions.math.hitbox.collider.local

import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.api.collider.local.ILocalOBB
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic.AABBPlus
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs

open class LocalOBB<T : ITargetLocation<*>>(
    halfExtents: Vector3d,
    localCenter: Vector3d,
    localRotation: Quaterniond,
    val parent: ICoordinateConverter
) : ILocalOBB<T> {

    private val globalCenter = Vector3d()
    private val globalRotation = Quaterniond()

    private val globalVertices = Array(8) { Vector3d() }
    private val globalAxes = Array(3) { Vector3d() }
    override val vertices: Array<Vector3d>
        get() {
            update()
            return globalVertices
        }
    override val axes: Array<Vector3d>
        get() {
            update()
            return globalAxes
        }
    private val fastExtents = Vector3d()
    private val fastBounds = AABBPlus<T>(fastExtents, globalCenter)

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
        get() {
            update()
            return globalCenter
        }
        set(center) {
            parent.update()
            parent.rotation.conjugate(Quaterniond()).transform(
                Vector3d(center).sub(parent.position),
                localCenter
            )
            globalCenter.set(center)
            dirty[0] = true
        }

    override var rotation: Quaterniond
        get() {
            update()
            return globalRotation
        }
        set(rotation) {
            parent.update()
            parent.rotation.conjugate(Quaterniond()).mul(rotation, localRotation)
            globalRotation.set(rotation)
            dirty[1] = true
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

        if (!dirty[0] && !dirty[1] && !dirty[2] && version[0] != parent.positionVersion() && version[1] == parent.rotationVersion()) {
            dirty[0] = false
            version[0] = parent.positionVersion()
            //仅进行位置移动
            val center = parent.rotation.transform(this.localCenter, Vector3d()).add(parent.position)
            val offset = center.sub(globalCenter, Vector3d())
            globalCenter.set(center)

            for (vertex in globalVertices) {
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
        rotation.transform(this.localCenter, globalCenter).add(position)
        rotation.mul(this.localRotation, globalRotation)
        val center = globalCenter

        // 旋转轴向
        globalRotation.transform(Vector3d(1.0, 0.0, 0.0), globalAxes[0])
        globalRotation.transform(Vector3d(0.0, 1.0, 0.0), globalAxes[1])
        globalRotation.transform(Vector3d(0.0, 0.0, 1.0), globalAxes[2])
        fastExtents.set(
            abs(globalAxes[0].x) * halfExtents.x + abs(globalAxes[1].x) * halfExtents.y + abs(globalAxes[2].x) * halfExtents.z,
            abs(globalAxes[0].y) * halfExtents.x + abs(globalAxes[1].y) * halfExtents.y + abs(globalAxes[2].y) * halfExtents.z,
            abs(globalAxes[0].z) * halfExtents.x + abs(globalAxes[1].z) * halfExtents.y + abs(globalAxes[2].z) * halfExtents.z
        )

        val v = Vector3d()

        // 计算顶点
        globalVertices[0].set(center).add(globalAxes[0].mul(halfExtents.x, v)).add(globalAxes[1].mul(halfExtents.y, v)).add(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[1].set(center).add(globalAxes[0].mul(halfExtents.x, v)).add(globalAxes[1].mul(halfExtents.y, v)).sub(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[2].set(center).add(globalAxes[0].mul(halfExtents.x, v)).sub(globalAxes[1].mul(halfExtents.y, v)).add(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[3].set(center).add(globalAxes[0].mul(halfExtents.x, v)).sub(globalAxes[1].mul(halfExtents.y, v)).sub(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[4].set(center).sub(globalAxes[0].mul(halfExtents.x, v)).add(globalAxes[1].mul(halfExtents.y, v)).add(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[5].set(center).sub(globalAxes[0].mul(halfExtents.x, v)).add(globalAxes[1].mul(halfExtents.y, v)).sub(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[6].set(center).sub(globalAxes[0].mul(halfExtents.x, v)).sub(globalAxes[1].mul(halfExtents.y, v)).add(globalAxes[2].mul(halfExtents.z, v))
        globalVertices[7].set(center).sub(globalAxes[0].mul(halfExtents.x, v)).sub(globalAxes[1].mul(halfExtents.y, v)).sub(globalAxes[2].mul(halfExtents.z, v))
    }

    override val fastCollider: IAABB<T>
        get() {
            update()
            return fastBounds
        }
}
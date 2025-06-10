package org.gitee.orryx.core.kether.actions.math.hitbox.collider.basic

import org.gitee.orryx.api.collider.ICollideFunction
import org.gitee.orryx.api.collider.IOBB
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.collections.ArrayList

class OBB<T : ITargetLocation<*>>(
    override var halfExtents: Vector3d,
    override var center: Vector3d,
    override var rotation: Quaterniond
) : IOBB<T> {

    private var disable = false

    override val vertices: Array<Vector3d>
        get() {
            val vertices = ArrayList<Vector3d>(8)

            // 计算局部坐标系下的顶点位置
            val localVertices = arrayOf(
                Vector3d(-halfExtents.x, -halfExtents.y, -halfExtents.z),
                Vector3d(halfExtents.x, -halfExtents.y, -halfExtents.z),
                Vector3d(halfExtents.x, halfExtents.y, -halfExtents.z),
                Vector3d(-halfExtents.x, halfExtents.y, -halfExtents.z),
                Vector3d(-halfExtents.x, -halfExtents.y, halfExtents.z),
                Vector3d(halfExtents.x, -halfExtents.y, halfExtents.z),
                Vector3d(halfExtents.x, halfExtents.y, halfExtents.z),
                Vector3d(-halfExtents.x, halfExtents.y, halfExtents.z)
            )

            // 将局部坐标系下的顶点转换到世界坐标系
            for (i in 0..7) {
                val vertex = localVertices[i]
                vertex.rotate(rotation)
                vertex.add(center)
                vertices[i] = vertex
            }

            return vertices.toTypedArray()
        }

    override val axes: Array<Vector3d>
        get() {
            val axes = arrayOf(
                Vector3d(1.0, 0.0, 0.0),
                Vector3d(0.0, 1.0, 0.0),
                Vector3d(0.0, 0.0, 1.0)
            )
            rotation.transform(axes[0])
            rotation.transform(axes[1])
            rotation.transform(axes[2])
            return axes
        }

    override fun setDisable(disable: Boolean) {
        this.disable = disable
    }

    override fun disable(): Boolean {
        return disable
    }

    override fun toString(): String {
        return "OBB{" +
                "halfExtents=" + halfExtents +
                ", center=" + center +
                ", rotation=" + rotation +
                ", disable=" + disable +
                '}'
    }
}

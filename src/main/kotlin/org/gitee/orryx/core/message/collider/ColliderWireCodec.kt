package org.gitee.orryx.core.message.collider

import com.google.common.io.ByteArrayDataOutput
import org.gitee.orryx.api.collider.IAABB
import org.gitee.orryx.api.collider.ICapsule
import org.gitee.orryx.api.collider.ICollider
import org.gitee.orryx.api.collider.IComposite
import org.gitee.orryx.api.collider.IOBB
import org.gitee.orryx.api.collider.IRay
import org.gitee.orryx.api.collider.ISphere
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.sqrt

/** Orryx 与 OrryxMod 的碰撞箱二进制协议编解码契约。 */
object ColliderWireCodec {

    const val MAX_ID_LENGTH = 1_024
    const val MAX_COMPOSITE_DEPTH = 3
    const val MAX_COMPOSITE_CHILDREN = 50
    const val MAX_COLLIDER_NODES = 200

    private const val MAX_COORDINATE = 30_000_000.0
    private const val MIN_DIMENSION = 0.01
    private const val MAX_RADIUS = 100.0
    private const val MAX_HALF_EXTENT = 100.0
    private const val MAX_HALF_HEIGHT = 100.0
    private const val MAX_RAY_LENGTH = 200.0
    private const val EPSILON = 1.0E-9
    private const val VERTICAL_EPSILON = 1.0E-6

    /**
     * 读取碰撞体当前状态并生成不可变快照。
     *
     * 根碰撞体被禁用时返回 null；复合体中被禁用的子碰撞体会被忽略。
     */
    fun snapshot(
        id: String,
        collider: ICollider<*>,
        color: ColliderRenderColor,
    ): ColliderWireSnapshot? {
        validateId(id, "碰撞箱 ID")
        if (collider.disable()) return null
        val budget = NodeBudget()
        val shape = toWireShape(collider, color, depth = 0, path = "", budget = budget) ?: return null
        return ColliderWireSnapshot(id, color, shape)
    }

    fun writeShowPayload(output: ByteArrayDataOutput, snapshot: ColliderWireSnapshot) {
        output.writeUTF(snapshot.id)
        output.writeInt(snapshot.shape.typeId)
        writeColor(output, snapshot.color)
        writeShapePayload(output, snapshot.shape)
    }

    fun writeUpdatePayload(output: ByteArrayDataOutput, snapshot: ColliderWireSnapshot) {
        output.writeUTF(snapshot.id)
        output.writeInt(snapshot.shape.typeId)
        writeShapePayload(output, snapshot.shape)
    }

    private fun toWireShape(
        collider: ICollider<*>,
        color: ColliderRenderColor,
        depth: Int,
        path: String,
        budget: NodeBudget,
    ): ColliderWireShape? {
        require(depth <= MAX_COMPOSITE_DEPTH) { "复合碰撞箱嵌套深度超过 $MAX_COMPOSITE_DEPTH" }
        if (collider.disable()) return null
        budget.consume()
        if (collider is ILocalCollider<*>) collider.update()

        return when (collider) {
            is ISphere<*> -> ColliderWireShape.Sphere(
                coordinate(collider.center.x, "球体中心 X"),
                coordinate(collider.center.y, "球体中心 Y"),
                coordinate(collider.center.z, "球体中心 Z"),
                bounded(collider.radius, MIN_DIMENSION, MAX_RADIUS, "球体半径"),
            )

            is IAABB<*> -> ColliderWireShape.Aabb(
                coordinate(collider.center.x, "AABB 中心 X"),
                coordinate(collider.center.y, "AABB 中心 Y"),
                coordinate(collider.center.z, "AABB 中心 Z"),
                bounded(collider.halfExtents.x, MIN_DIMENSION, MAX_HALF_EXTENT, "AABB 半轴 X"),
                bounded(collider.halfExtents.y, MIN_DIMENSION, MAX_HALF_EXTENT, "AABB 半轴 Y"),
                bounded(collider.halfExtents.z, MIN_DIMENSION, MAX_HALF_EXTENT, "AABB 半轴 Z"),
            )

            is IOBB<*> -> {
                val quaternion = normalizedQuaternion(collider.rotation)
                ColliderWireShape.Obb(
                    coordinate(collider.center.x, "OBB 中心 X"),
                    coordinate(collider.center.y, "OBB 中心 Y"),
                    coordinate(collider.center.z, "OBB 中心 Z"),
                    bounded(collider.halfExtents.x, MIN_DIMENSION, MAX_HALF_EXTENT, "OBB 半轴 X"),
                    bounded(collider.halfExtents.y, MIN_DIMENSION, MAX_HALF_EXTENT, "OBB 半轴 Y"),
                    bounded(collider.halfExtents.z, MIN_DIMENSION, MAX_HALF_EXTENT, "OBB 半轴 Z"),
                    quaternion.x,
                    quaternion.y,
                    quaternion.z,
                    quaternion.w,
                )
            }

            is ICapsule<*> -> capsuleShape(collider)

            is IRay<*> -> {
                val direction = normalizedDirection(collider.direction, Vector3d(0.0, 0.0, 1.0))
                ColliderWireShape.Ray(
                    coordinate(collider.origin.x, "射线起点 X"),
                    coordinate(collider.origin.y, "射线起点 Y"),
                    coordinate(collider.origin.z, "射线起点 Z"),
                    direction.x,
                    direction.y,
                    direction.z,
                    bounded(collider.length, MIN_DIMENSION, MAX_RAY_LENGTH, "射线长度"),
                )
            }

            is IComposite<*, *> -> {
                require(depth < MAX_COMPOSITE_DEPTH) {
                    "复合碰撞箱嵌套深度超过 $MAX_COMPOSITE_DEPTH"
                }
                require(collider.collidersCount <= MAX_COMPOSITE_CHILDREN) {
                    "复合碰撞箱子节点数量超过 $MAX_COMPOSITE_CHILDREN"
                }
                val children = buildList {
                    for (index in 0 until collider.collidersCount) {
                        val child = collider.getCollider(index)
                        val childPath = if (path.isEmpty()) index.toString() else "$path.$index"
                        val childShape = toWireShape(child, color, depth + 1, childPath, budget) ?: continue
                        validateId(childPath, "复合碰撞箱子节点 ID")
                        add(ColliderWireChild(childPath, color, childShape))
                    }
                }
                ColliderWireShape.Composite(children)
            }

            else -> throw IllegalArgumentException("不支持的碰撞箱实现: ${collider::class.qualifiedName}")
        }
    }

    private fun capsuleShape(collider: ICapsule<*>): ColliderWireShape {
        val centerX = coordinate(collider.center.x, "胶囊体中心 X")
        val centerY = coordinate(collider.center.y, "胶囊体中心 Y")
        val centerZ = coordinate(collider.center.z, "胶囊体中心 Z")
        val radius = bounded(collider.radius, MIN_DIMENSION, MAX_RADIUS, "胶囊体半径")
        val halfHeight = bounded(collider.height / 2.0, MIN_DIMENSION, MAX_HALF_HEIGHT, "胶囊体半高")
        val direction = normalizedDirection(collider.direction, Vector3d(0.0, 1.0, 0.0))
        if (abs(direction.x) <= VERTICAL_EPSILON && abs(direction.z) <= VERTICAL_EPSILON) {
            return ColliderWireShape.Capsule(centerX, centerY, centerZ, radius, halfHeight)
        }
        val quaternion = normalizedQuaternion(collider.rotation)
        return ColliderWireShape.OrientedCapsule(
            centerX,
            centerY,
            centerZ,
            radius,
            halfHeight,
            quaternion.x,
            quaternion.y,
            quaternion.z,
            quaternion.w,
        )
    }

    private fun writeShapePayload(output: ByteArrayDataOutput, shape: ColliderWireShape) {
        when (shape) {
            is ColliderWireShape.Sphere -> {
                output.writeDouble(shape.centerX)
                output.writeDouble(shape.centerY)
                output.writeDouble(shape.centerZ)
                output.writeDouble(shape.radius)
            }

            is ColliderWireShape.Aabb -> {
                output.writeDouble(shape.centerX)
                output.writeDouble(shape.centerY)
                output.writeDouble(shape.centerZ)
                output.writeDouble(shape.halfX)
                output.writeDouble(shape.halfY)
                output.writeDouble(shape.halfZ)
            }

            is ColliderWireShape.Obb -> {
                output.writeDouble(shape.centerX)
                output.writeDouble(shape.centerY)
                output.writeDouble(shape.centerZ)
                output.writeDouble(shape.halfX)
                output.writeDouble(shape.halfY)
                output.writeDouble(shape.halfZ)
                output.writeFloat(shape.quaternionX)
                output.writeFloat(shape.quaternionY)
                output.writeFloat(shape.quaternionZ)
                output.writeFloat(shape.quaternionW)
            }

            is ColliderWireShape.Capsule -> {
                output.writeDouble(shape.centerX)
                output.writeDouble(shape.centerY)
                output.writeDouble(shape.centerZ)
                output.writeDouble(shape.radius)
                output.writeDouble(shape.halfHeight)
            }

            is ColliderWireShape.OrientedCapsule -> {
                output.writeDouble(shape.centerX)
                output.writeDouble(shape.centerY)
                output.writeDouble(shape.centerZ)
                output.writeDouble(shape.radius)
                output.writeDouble(shape.halfHeight)
                output.writeFloat(shape.quaternionX)
                output.writeFloat(shape.quaternionY)
                output.writeFloat(shape.quaternionZ)
                output.writeFloat(shape.quaternionW)
            }

            is ColliderWireShape.Ray -> {
                output.writeDouble(shape.originX)
                output.writeDouble(shape.originY)
                output.writeDouble(shape.originZ)
                output.writeDouble(shape.directionX)
                output.writeDouble(shape.directionY)
                output.writeDouble(shape.directionZ)
                output.writeDouble(shape.length)
            }

            is ColliderWireShape.Composite -> {
                output.writeInt(shape.children.size)
                shape.children.forEach { child ->
                    output.writeUTF(child.id)
                    output.writeInt(child.shape.typeId)
                    writeColor(output, child.color)
                    writeShapePayload(output, child.shape)
                }
            }
        }
    }

    private fun writeColor(output: ByteArrayDataOutput, color: ColliderRenderColor) {
        output.writeInt(color.r)
        output.writeInt(color.g)
        output.writeInt(color.b)
        output.writeInt(color.a)
    }

    private fun coordinate(value: Double, name: String): Double {
        return bounded(value, -MAX_COORDINATE, MAX_COORDINATE, name)
    }

    private fun bounded(value: Double, min: Double, max: Double, name: String): Double {
        require(value.isFinite()) { "$name 必须是有限值" }
        return value.coerceIn(min, max)
    }

    private fun normalizedDirection(direction: Vector3d, fallback: Vector3d): Vector3d {
        require(direction.x.isFinite() && direction.y.isFinite() && direction.z.isFinite()) {
            "碰撞箱方向必须是有限值"
        }
        val lengthSquared = direction.x * direction.x + direction.y * direction.y + direction.z * direction.z
        if (!lengthSquared.isFinite() || lengthSquared <= EPSILON) return Vector3d(fallback)
        val inverseLength = 1.0 / sqrt(lengthSquared)
        return Vector3d(
            (direction.x * inverseLength).coerceIn(-1.0, 1.0),
            (direction.y * inverseLength).coerceIn(-1.0, 1.0),
            (direction.z * inverseLength).coerceIn(-1.0, 1.0),
        )
    }

    private fun normalizedQuaternion(quaternion: Quaterniond): WireQuaternion {
        require(
            quaternion.x.isFinite() && quaternion.y.isFinite() &&
                quaternion.z.isFinite() && quaternion.w.isFinite()
        ) { "碰撞箱四元数必须是有限值" }
        val lengthSquared = quaternion.x * quaternion.x + quaternion.y * quaternion.y +
            quaternion.z * quaternion.z + quaternion.w * quaternion.w
        if (!lengthSquared.isFinite() || lengthSquared <= EPSILON) {
            return WireQuaternion(0f, 0f, 0f, 1f)
        }
        val inverseLength = 1.0 / sqrt(lengthSquared)
        return WireQuaternion(
            (quaternion.x * inverseLength).toFloat(),
            (quaternion.y * inverseLength).toFloat(),
            (quaternion.z * inverseLength).toFloat(),
            (quaternion.w * inverseLength).toFloat(),
        )
    }

    private fun validateId(id: String, name: String) {
        require(id.isNotBlank()) { "$name 不能为空" }
        require(id.length <= MAX_ID_LENGTH) { "$name 长度不能超过 $MAX_ID_LENGTH" }
    }

    private class NodeBudget {

        private var nodes = 0

        fun consume() {
            nodes++
            require(nodes <= MAX_COLLIDER_NODES) { "碰撞箱节点数量超过 $MAX_COLLIDER_NODES" }
        }
    }

    private data class WireQuaternion(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float,
    )
}

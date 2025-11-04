package org.gitee.orryx.utils

import io.lumine.xikage.mythicmobs.adapters.AbstractLocation
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.raytrace.FluidHandling
import org.gitee.orryx.utils.raytrace.RayTraceResult
import org.gitee.orryx.utils.raytrace.SpigotWorld
import org.joml.Matrix3dc
import org.joml.Vector3d
import org.joml.Vector3dc
import taboolib.common5.cdouble
import taboolib.module.effect.math.Matrix

val X_AXIS = Vector(1.0, 0.0, 0.0)
val Y_AXIS = Vector(0.0, 1.0, 0.0)
val Z_AXIS = Vector(0.0, 0.0, 1.0)

/**
 * AbstractLocation是否面相loc2
 */
internal fun AbstractLocation.isFace(target: AbstractLocation?): Boolean {
    val vectorA = clone().toVector()
    val vectorB = target?.clone()?.toVector() ?: return false
    val vector = vectorA.subtract(vectorB)
    return vector.angle(direction) > Math.PI/2
}

/**
 * Location是否面相loc2
 */
internal fun Location.isFace(target: Location?): Boolean {
    val vectorA = clone().toVector()
    val vectorB = target?.clone()?.toVector() ?: return false
    val vector = vectorA.subtract(vectorB)
    return vector.angle(direction) > Math.PI/2
}

/**
 * 判断点是否在圆圈内
 * */
internal fun Location.isInRound(origin: Location, radius: Double): Boolean {
    return clone().also { it.y = 0.0 }.distance(origin.clone().also { it.y = 0.0 }) <= radius
}

/**
 * 获得实体的方向向量
 * @param x 前方
 * @param y 上方
 * @param z 右方
 * */
internal fun LivingEntity.direction(x: Double, y: Double, z: Double, pitch: Boolean = false) = location.direction.direction(x, y, z, pitch)

/**
 * 获得目标的方向向量
 * @param x 前方
 * @param y 上方
 * @param z 右方
 * */
internal fun ITargetLocation<*>.direction(x: Double, y: Double, z: Double, pitch: Boolean = false) = location.direction.direction(x, y, z, pitch)

/**
 * 获得目标的方向向量
 * @param x 前方
 * @param y 上方
 * @param z 右方
 * */
internal fun ITargetEntity<*>.direction(x: Double, y: Double, z: Double, pitch: Boolean = false) = entity.location.direction.direction(x, y, z, pitch)

internal fun Vector.direction(x: Double, y: Double, z: Double, pitch: Boolean): Vector {
    return if (pitch) {
        val zV = clone().setY(0).normalize().crossProduct(Vector(0, 1, 0)).normalize()
        val xV = clone().normalize()
        val yV = zV.clone().crossProduct(xV)
        xV.multiply(x).add(zV.multiply(z)).add(yV.multiply(y))
    } else {
        val xV = clone().setY(0).normalize()
        val zV = xV.clone().crossProduct(Vector(0, 1, 0))
        xV.multiply(x).add(Vector(0.0, y, 0.0)).add(zV.multiply(z))
    }
}

fun Location.joml() = Vector3d(x, y, z)

fun taboolib.common.util.Location.joml() = Vector3d(x, y, z)

fun Vector.joml() = Vector3d(x, y, z)

fun taboolib.common.util.Vector.joml() = Vector3d(x, y, z)

fun Vector3dc.bukkit() = Vector(x(), y(), z())

fun Vector.abstract() = AbstractVector(joml())

fun taboolib.common.util.Vector.abstract() = AbstractVector(joml())

fun Vector3d.abstract() = AbstractVector(this)

fun Vector3dc.taboo() = taboolib.common.util.Vector(x(), y(), z())

fun IVector.bukkit() = Vector(joml.x, joml.y, joml.z)

fun IVector.taboo() = taboolib.common.util.Vector(joml.x, joml.y, joml.z)

fun IVector.joml() = joml

fun String.parseVector(): AbstractVector? {
    val list = split(",")
    return kotlin.runCatching {
        AbstractVector(list[0].cdouble, list[1].cdouble, list[2].cdouble)
    }.getOrNull()
}

fun IVector.commaJoinString() = "${x()},${y()},${z()}"

fun Matrix3dc.taboo() = Matrix(
    arrayOf(
        doubleArrayOf(m00(), m01(), m02()),
        doubleArrayOf(m10(), m11(), m12()),
        doubleArrayOf(m20(), m21(), m22())
    )
)

data class AABB(val minVector3d: Vector3dc, val maxVector3d: Vector3dc) {

    val minX
        get() = minVector3d.x()

    val minY
        get() = minVector3d.y()

    val minZ
        get() = minVector3d.z()

    val maxX
        get() = maxVector3d.x()

    val maxY
        get() = maxVector3d.y()

    val maxZ
        get() = maxVector3d.z()

    constructor(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) : this(Vector3d(minX, minY, minZ), Vector3d(maxX, maxY, maxZ))

}

fun getEntityAABB(entity: Entity): AABB {
    return getManualAABB(entity)
}

private fun getManualAABB(entity: Entity): AABB {
    val loc = entity.location
    val (width, height) = entity.width to entity.height
    return AABB(loc.x - width / 2, loc.y, loc.z - width / 2, loc.x + width / 2, loc.y + height, loc.z + width / 2)
}

/**
 * 检测两碰撞箱是否碰撞
 * */
fun areAABBsColliding(aabb0: AABB, aabb1: AABB): Boolean {
    // 检查每个轴是否有重叠
    val xOverlap = aabb0.minX <= aabb1.maxX && aabb0.maxX >= aabb1.minX
    val yOverlap = aabb0.minY <= aabb1.maxY && aabb0.maxY >= aabb1.minY
    val zOverlap = aabb0.minZ <= aabb1.maxZ && aabb0.maxZ >= aabb1.minZ

    // 所有轴都有重叠时返回 true
    return xOverlap && yOverlap && zOverlap
}

/**
 * 光线追踪碰撞计算
 * @param start 原点向量
 * @param direction 射线向量
 * @param maxDistance 碰撞范围（1.12.2及以下无效）
 * @param fluidHandling 流体处理
 * @param checkAxisAlignedBB 是否比对轴对称包围盒
 * @param returnClosestPos 即使光线未命中任何可碰撞方块，也会返回光线路径中最后的落点
 * */
fun World.rayTraceBlocks(
    start: Vector3dc,
    direction: Vector3dc,
    maxDistance: Double = 1.0,
    fluidHandling: FluidHandling = FluidHandling.NONE,
    checkAxisAlignedBB: Boolean = true,
    returnClosestPos: Boolean = true
): RayTraceResult? {
    return SpigotWorld(this).rayTraceBlocks(start, direction, maxDistance, fluidHandling, checkAxisAlignedBB, returnClosestPos)
}
package org.gitee.orryx.utils

import io.lumine.xikage.mythicmobs.adapters.AbstractLocation
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import taboolib.module.effect.math.Matrix

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
internal fun LivingEntity.direction(x: Double, y: Double, z: Double): Vector {
    val xV = eyeLocation.direction.clone().setY(0).normalize()
    val zV = xV.clone().crossProduct(Vector(0, 1, 0)).normalize()
    return Vector(0, 0, 0).add(xV.multiply(x)).add(Vector(0.0, y, 0.0)).add(zV.multiply(z))
}

/**
 * 获得目标的方向向量
 * @param x 前方
 * @param y 上方
 * @param z 右方
 * */
internal fun ITargetLocation<*>.direction(x: Double, y: Double, z: Double): Vector {
    val xV = eyeLocation.direction.clone().setY(0).normalize()
    val zV = xV.clone().crossProduct(Vector(0, 1, 0)).normalize()
    return Vector(0, 0, 0).add(xV.multiply(x)).add(Vector(0.0, y, 0.0)).add(zV.multiply(z))
}

/**
 * 获得目标的方向向量
 * @param x 前方
 * @param y 上方
 * @param z 右方
 * */
internal fun ITargetEntity<*>.direction(x: Double, y: Double, z: Double): Vector {
    val xV = entity.eyeLocation.direction.clone().setY(0).normalize()
    val zV = xV.clone().crossProduct(Vector(0, 1, 0)).normalize()
    return Vector(0, 0, 0).add(xV.multiply(x)).add(Vector(0.0, y, 0.0)).add(zV.multiply(z))
}

fun Vector.joml() = Vector3d(x, y, z)

fun Vector3d.bukkit() = Vector(x, y, z)

fun Vector.abstract() = AbstractVector(joml())

fun Vector3d.abstract() = AbstractVector(this)

fun IVector.bukkit() = Vector(joml.x, joml.y, joml.z)

fun IVector.joml() = joml

fun Matrix4d.taboo() = Matrix(
    arrayOf(
        doubleArrayOf(m00(), m01(), m02(), m03()),
        doubleArrayOf(m10(), m11(), m12(), m13()),
        doubleArrayOf(m20(), m21(), m22(), m23()),
        doubleArrayOf(m30(), m31(), m32(), m33())
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

    constructor(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) : this(Vector3d(minX, minY, minZ), Vector3d(minX, minY, minZ))

}

fun getEntityAABB(entity: Entity): AABB {
    return getManualAABB(entity)
}

private fun getManualAABB(entity: Entity): AABB {
    val loc = entity.location.toVector()
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
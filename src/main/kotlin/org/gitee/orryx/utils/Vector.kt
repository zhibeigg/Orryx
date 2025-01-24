package org.gitee.orryx.utils

import io.lumine.xikage.mythicmobs.adapters.AbstractLocation
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.util.Vector
import org.gitee.orryx.core.targets.ITarget
import org.joml.Vector3d

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
 * 判断点是否在立方体内
 */
internal fun isPointInsideCuboid(point: Location, corners: Array<Location>, width: Double, height: Double): Boolean {
    val xs = corners.map { it.x }
    val ys = corners.map { it.y }
    val zs = corners.map { it.z }

    val offset = width / 2

    val minX = point.x - offset
    val maxX = point.x + offset

    val minY = point.y
    val maxY = point.y + height

    val minZ = point.z - offset
    val maxZ = point.z + offset

    val under = under(minX, xs.maxOrNull()!!) && under(minY, ys.maxOrNull()!!) && under(minZ, zs.maxOrNull()!!)
    val over = over(maxX, xs.minOrNull()!!) && over(maxY, ys.minOrNull()!!) && over(maxZ, zs.minOrNull()!!)

    return under && over
}

/**
 * 判断点是否在圆圈内
 * */
internal fun Location.isInRound(origin: Location, radius: Double): Boolean {
    return clone().also { it.y = 0.0 }.distance(origin.clone().also { it.y = 0.0 }) <= radius
}

private fun under(v: Double, top: Double): Boolean = v <= top

private fun over(v: Double, floor: Double): Boolean = v >= floor

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
internal fun ITarget<*>.direction(x: Double, y: Double, z: Double): Vector {
    val xV = eyeLocation.direction.clone().setY(0).normalize()
    val zV = xV.clone().crossProduct(Vector(0, 1, 0)).normalize()
    return Vector(0, 0, 0).add(xV.multiply(x)).add(Vector(0.0, y, 0.0)).add(zV.multiply(z))
}

fun Vector.joml() = Vector3d(x, y, z)

fun Vector3d.bukkit() = Vector(x, y, z)

fun ro() {
    Vector3d()
}
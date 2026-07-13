package org.gitee.orryx.utils

import org.gitee.orryx.api.collider.*
import org.gitee.orryx.api.collider.local.ICoordinateConverter
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.TargetCoordinateConverter
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Intersectiond
import org.joml.Math.clamp
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 判断两个碰撞箱是否相交
 *
 * 适用于碰撞箱处于不同的坐标系中，例如A、B实体存在以实体自身为中心的碰撞箱，
 * 此时需要使用坐标变换栈将碰撞箱转换为世界坐标。
 *
 * ```
 * val A: Entity = ...
 * val B: Entity = ...
 * // 假设有数据附加能获取碰撞箱
 * colliderA: ICollider = A.getData(...)
 * colliderB: ICollider = B.getData(...)
 * // 判断碰撞箱是否相交
 * boolean result = ColliderUtil.isColliding(colliderA, colliderB)
 * ```
 *
 * @param collider 任意碰撞箱
 * @param other    另一个碰撞箱
 * @return 相交返回true
 */
@Suppress("UNCHECKED_CAST")
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> colliding(collider: ICollider<T1>, other: ICollider<T2>?): Boolean {
    if (other == null) return false
    if (collider.disable() || other.disable()) return false
    if (other === collider) return true
    val fastCollider1 = collider.fastCollider
    val fastCollider2 = other.fastCollider
    // 快速碰撞检测
    if (fastCollider1 != null && fastCollider2 != null) {
        val colliding = isColliding(fastCollider1, fastCollider2)
        if (!colliding) {
            return false
        }
    }
    return when (collider.type) {
        ColliderType.OBB -> when (other.type) {
            ColliderType.OBB -> isColliding(collider as IOBB, other as IOBB)
            ColliderType.SPHERE -> isColliding(other as ISphere, collider as IOBB)
            ColliderType.CAPSULE -> isColliding(other as ICapsule, collider as IOBB)
            ColliderType.AABB -> isColliding(collider as IOBB, other as IAABB)
            ColliderType.RAY -> isColliding(other as IRay, collider as IOBB)
            ColliderType.COMPOSITE -> isColliding(other as IComposite<T2, ICollider<T2>>, collider)
            ColliderType.NONE -> false
        }
        ColliderType.SPHERE -> when (other.type) {
            ColliderType.OBB -> isColliding(collider as ISphere, other as IOBB)
            ColliderType.SPHERE -> isColliding(collider as ISphere, other as ISphere)
            ColliderType.CAPSULE -> isColliding(other as ICapsule, collider as ISphere)
            ColliderType.AABB -> isColliding(collider as ISphere, other as IAABB)
            ColliderType.RAY -> isColliding(other as IRay, collider as ISphere)
            ColliderType.COMPOSITE -> isColliding(other as IComposite<T2, ICollider<T2>>, collider)
            ColliderType.NONE -> false
        }
        ColliderType.CAPSULE -> when (other.type) {
            ColliderType.OBB -> isColliding(collider as ICapsule, other as IOBB)
            ColliderType.SPHERE -> isColliding(collider as ICapsule, other as ISphere)
            ColliderType.CAPSULE -> isColliding(collider as ICapsule, other as ICapsule)
            ColliderType.AABB -> isColliding(collider as ICapsule, other as IAABB)
            ColliderType.RAY -> isColliding(other as IRay, collider as ICapsule)
            ColliderType.COMPOSITE -> isColliding(other as IComposite<T2, ICollider<T2>>, collider)
            ColliderType.NONE -> false
        }
        ColliderType.AABB -> when (other.type) {
            ColliderType.OBB -> isColliding(other as IOBB, collider as IAABB)
            ColliderType.SPHERE -> isColliding(other as ISphere, collider as IAABB)
            ColliderType.CAPSULE -> isColliding(other as ICapsule, collider as IAABB)
            ColliderType.AABB -> isColliding(collider as IAABB, other as IAABB)
            ColliderType.RAY -> isColliding(other as IRay, collider as IAABB)
            ColliderType.COMPOSITE -> isColliding(other as IComposite<T2, ICollider<T2>>, collider)
            ColliderType.NONE -> false
        }
        ColliderType.RAY -> when (other.type) {
            ColliderType.OBB -> isColliding(collider as IRay, other as IOBB)
            ColliderType.SPHERE -> isColliding(collider as IRay, other as ISphere)
            ColliderType.CAPSULE -> isColliding(collider as IRay, other as ICapsule)
            ColliderType.AABB -> isColliding(collider as IRay, other as IAABB)
            ColliderType.RAY -> isColliding(collider as IRay, other as IRay)
            ColliderType.COMPOSITE -> isColliding(other as IComposite<T2, ICollider<T2>>, collider)
            ColliderType.NONE -> false
        }
        ColliderType.COMPOSITE -> isColliding(collider as IComposite<T2, ICollider<T2>>, other)
        ColliderType.NONE -> false
    }
}

/** 判断两个胶囊体是否碰撞
 *
 * @param capsule 胶囊体
 * @param other   胶囊体
 * @return 碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(capsule: ICapsule<T1>, other: ICapsule<T2>): Boolean {
    // 计算头尾点最值
    var h = capsule.height / 2
    val pointA1 = capsule.direction.mul(h, Vector3d()).add(capsule.center)
    val pointA2 = capsule.direction.mul(-h, Vector3d()).add(capsule.center)
    h = other.height / 2
    val pointB1 = other.direction.mul(h, Vector3d()).add(other.center)
    val pointB2 = other.direction.mul(-h, Vector3d()).add(other.center)
    // 求两条线段的最短距离
    val distance = getClosestDistanceBetweenSegmentsSqr(pointA1, pointA2, pointB1, pointB2)
    // 求两个球半径和
    val totalRadius = square(capsule.radius + other.radius)
    // 距离小于等于半径和则碰撞
    return distance <= totalRadius
}

/**
 * 判断胶囊体与球体是否碰撞
 *
 * @param capsule 胶囊体
 * @param sphere  球体
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(capsule: ICapsule<T1>, sphere: ISphere<T2>): Boolean {
    // 计算头尾点最值
    val height = capsule.height / 2
    val point1 = capsule.direction.mul(height, Vector3d()).add(capsule.center)
    val point2 = capsule.direction.mul(-height, Vector3d()).add(capsule.center)
    val closest = getClosestPointOnSegment(point1, point2, sphere.center)
    // 求两个球半径和
    val totalRadius = square(capsule.radius + sphere.radius)
    // 球两个球心之间的距离
    val distance = closest.sub(sphere.center).lengthSquared()
    // 距离小于等于半径和则碰撞
    return distance <= totalRadius
}

/**
 * 判断胶囊体与OBB是否碰撞
 *
 * @param capsule 胶囊体
 * @param obb     OBB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(capsule: ICapsule<T1>, obb: IOBB<T2>): Boolean {
    // 计算头尾点最值
    val height = capsule.height / 2
    val point1 = capsule.direction.mul(height, Vector3d()).add(capsule.center)
    val point2 = capsule.direction.mul(-height, Vector3d()).add(capsule.center)
    val closest1 = getClosestPointOnSegment(point1, point2, obb.center)
    val closest2 = getClosestPointOBB(closest1, obb)
    // 求胶囊体半径平方
    val totalRadius = square(capsule.radius)
    // 求两个点之间的距离
    val distance = (closest1.sub(closest2)).lengthSquared()
    // 距离小于等于半径平方则碰撞
    return distance <= totalRadius
}

/**
 * 判断OBB盒与OBB盒是否碰撞
 *
 * @param obb   OBB盒
 * @param other OBB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(obb: IOBB<T1>, other: IOBB<T2>): Boolean {
    val axes1 = obb.axes
    val axes2 = other.axes
    return Intersectiond.testObOb(
        obb.center,
        axes1[0],
        axes1[1],
        axes1[2],
        obb.halfExtents,
        other.center,
        axes2[0],
        axes2[1],
        axes2[2],
        other.halfExtents
    )
}

/**
 * 判断球体与OBB是否碰撞
 *
 * @param sphere 球体
 * @param obb    OBB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(sphere: ISphere<T1>, obb: IOBB<T2>): Boolean {
    // 求最近点
    val nearP = getClosestPointOBB(sphere.center, obb)
    // 与 AABB 检测原理相同
    val distance = nearP.sub(sphere.center).lengthSquared()
    val radius = square(sphere.radius)
    return distance <= radius
}

/**
 * 判断球体与球体是否碰撞
 *
 * @param sphere 球体
 * @param other  球体
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(sphere: ISphere<T1>, other: ISphere<T2>): Boolean {
    return Intersectiond.testSphereSphere(sphere.center, square(sphere.radius), other.center, square(other.radius))
}

/**
 * 判断球体与AABB盒是否碰撞
 *
 * @param sphere 球体
 * @param aabb   AABB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(sphere: ISphere<T1>, aabb: IAABB<T2>): Boolean {
    return Intersectiond.testAabSphere(aabb.min, aabb.max, sphere.center, square(sphere.radius))
}

/**
 * 判断胶囊体与AABB盒是否碰撞
 *
 * @param capsule 胶囊体
 * @param aabb    AABB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(capsule: ICapsule<T1>, aabb: IAABB<T2>): Boolean {
    // 计算头尾点最值
    val height = capsule.height / 2
    val pointA1 = capsule.direction.mul(height, Vector3d()).add(capsule.center)
    val pointA2 = capsule.direction.mul(-height, Vector3d()).add(capsule.center)
    val closest1 = getClosestPointOnSegment(pointA1, pointA2, aabb.center)
    val closest2 = getClosestPointAABB(closest1, aabb)
    // 求胶囊体半径平方
    val totalRadius = square(capsule.radius)
    // 求两个点之间的距离
    val distance = closest1.sub(closest2).lengthSquared()
    // 距离小于等于半径平方则碰撞
    return distance <= totalRadius
}

/**
 * 判断有限线段与 AABB 是否碰撞。
 *
 * 参数 t 被限制在 [0, 1]，不会把线段末端之外的无限延长线算作命中。
 */
fun segmentIntersectsAabb(start: Vector3d, end: Vector3d, min: Vector3d, max: Vector3d): Boolean {
    var near = 0.0
    var far = 1.0

    fun clip(startValue: Double, delta: Double, minValue: Double, maxValue: Double): Boolean {
        if (abs(delta) <= 1e-12) {
            return startValue in minValue..maxValue
        }
        var first = (minValue - startValue) / delta
        var second = (maxValue - startValue) / delta
        if (first > second) {
            val swap = first
            first = second
            second = swap
        }
        near = max(near, first)
        far = min(far, second)
        return near <= far
    }

    return clip(start.x, end.x - start.x, min.x, max.x) &&
        clip(start.y, end.y - start.y, min.y, max.y) &&
        clip(start.z, end.z - start.z, min.z, max.z)
}

private fun <T: ITargetLocation<*>> rayEnd(ray: IRay<T>): Vector3d {
    return Vector3d(ray.direction).mul(ray.length).add(ray.origin)
}

/**
 * 判断有限射线段与AABB盒是否碰撞
 *
 * @param ray  有限射线段
 * @param aabb AABB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(ray: IRay<T1>, aabb: IAABB<T2>): Boolean {
    return segmentIntersectsAabb(ray.origin, rayEnd(ray), aabb.min, aabb.max)
}

/**
 * 判断射线与球体是否碰撞
 *
 * @param ray    射线
 * @param sphere 球体
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(ray: IRay<T1>, sphere: ISphere<T2>): Boolean {
    val origin = ray.origin
    val end = rayEnd(ray)
    val center = sphere.center
    return Intersectiond.testLineSegmentSphere(
        origin.x, origin.y, origin.z,
        end.x, end.y, end.z,
        center.x, center.y, center.z,
        square(sphere.radius)
    )
}

/**
 * 判断射线与OBB盒是否碰撞
 *
 * @param ray 射线
 * @param obb OBB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(ray: IRay<T1>, obb: IOBB<T2>): Boolean {
    val center = obb.center
    val axes = obb.axes
    val halfExtents = obb.halfExtents

    fun toLocal(point: Vector3d): Vector3d {
        val offset = point.sub(center, Vector3d())
        return Vector3d(
            offset.dot(axes[0]),
            offset.dot(axes[1]),
            offset.dot(axes[2])
        )
    }

    val localStart = toLocal(ray.origin)
    val localEnd = toLocal(rayEnd(ray))
    return segmentIntersectsAabb(
        localStart,
        localEnd,
        Vector3d(-halfExtents.x, -halfExtents.y, -halfExtents.z),
        halfExtents
    )
}

/**
 * 判断射线与胶囊体是否碰撞
 *
 * @param ray     射线
 * @param capsule 胶囊体
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(ray: IRay<T1>, capsule: ICapsule<T2>): Boolean {
    val halfHeight = capsule.height / 2.0
    val startPoint = capsule.direction.mul(-halfHeight, Vector3d()).add(capsule.center)
    val endPoint = capsule.direction.mul(halfHeight, Vector3d()).add(capsule.center)
    val sqr = getClosestDistanceBetweenSegmentsSqr(ray.origin, rayEnd(ray), startPoint, endPoint)
    return sqr <= square(capsule.radius)
}

/**
 * 判断射线与射线是否碰撞<br></br>
 * 这有必要吗🤣
 *
 * @param ray   射线
 * @param other 射线
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(ray: IRay<T1>, other: IRay<T2>): Boolean {
    return isSegmentCross(ray.origin, rayEnd(ray), other.origin, rayEnd(other))
}

/**
 * 判断两个AABB盒是否碰撞
 *
 * @param aabb  AABB盒
 * @param other AABB盒
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(aabb: IAABB<T1>, other: IAABB<T2>): Boolean {
    return Intersectiond.testAabAab(aabb.min, aabb.max, other.min, other.max)
}

fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(obb: IOBB<T1>, aabb: IAABB<T2>): Boolean {
    val obbCenter = obb.center
    val obbAxes = obb.axes
    val obbHalfExtents = obb.halfExtents
    val aabbCenter = aabb.center
    val aabbHalfExtents = aabb.halfExtents
    return Intersectiond.testObOb(
        obbCenter.x, obbCenter.y, obbCenter.z,
        obbAxes[0].x, obbAxes[0].y, obbAxes[0].z,
        obbAxes[1].x, obbAxes[1].y, obbAxes[1].z,
        obbAxes[2].x, obbAxes[2].y, obbAxes[2].z,
        obbHalfExtents.x, obbHalfExtents.y, obbHalfExtents.z,
        aabbCenter.x, aabbCenter.y, aabbCenter.z,
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
        aabbHalfExtents.x, aabbHalfExtents.y, aabbHalfExtents.z
    )
}

/**
 * 判断复合碰撞箱与其他碰撞体是否碰撞
 *
 * @param composite 复合碰撞箱
 * @param other     其他碰撞体
 * @return 有碰撞返回true
 */
fun <T1: ITargetLocation<*>, T2: ITargetLocation<*>> isColliding(composite: IComposite<T1, ICollider<T1>>, other: ICollider<T2>): Boolean {
    val count = composite.collidersCount
    for (i in 0..<count) {
        if (colliding(composite.getCollider(i), other)) {
            return true
        }
    }
    return false
}

/**
 * 获取线段上最近待判定点的坐标
 *
 * @param start 线段起点
 * @param end   线段终点
 * @param point 待判定点
 * @return 线段上最接近判定点的坐标
 */
fun getClosestPointOnSegment(start: Vector3d, end: Vector3d, point: Vector3d): Vector3d {
    val se = end.sub(start, Vector3d())
    val sp = point.sub(start, Vector3d())
    var f = se.dot(sp) / se.lengthSquared()
    f = min(max(f, 0.0), 1.0)
    return se.mul(f).add(start, sp)
}

/**
 * 判断线段相交
 *
 * @param start1 线段 1 起点
 * @param end1   线段 1 终点
 * @param start2 线段 2 起点
 * @param end2   线段 2 终点
 * @return 相交返回true
 */
private fun isSegmentCross(start1: Vector3d, end1: Vector3d, start2: Vector3d, end2: Vector3d): Boolean {
    if (min(start1.x, end1.x) - max(start2.x, end2.x) > 0.01 || min(start1.y, end1.y) - max(start2.y, end2.y) > 0.01 
        || min(start1.z, end1.z) - max(start2.z, end2.z) > 0.01 || min(start2.x, end2.x) - max(start1.x, end1.x) > 0.01 
        || min(start2.y, end2.y) - max(start1.y, end1.y) > 0.01 || min(start2.z, end2.z) - max(start1.z, end1.z) > 0.01) {
        return false
    }
    val line1 = end1.sub(start1, Vector3d())
    val line2 = end2.sub(start2, Vector3d())
    val v1 = Vector3d()
    val v2 = Vector3d()
    return line1.cross(start2.sub(start1, v1), v1).normalize() != line1.cross(end2.sub(start1, v2), v2).normalize()
            && line2.cross(start1.sub(start2, v1), v1).normalize() != line2.cross(end1.sub(start2, v2), v2).normalize()
}

/**
 * 计算两个线段最近距离的平方
 *
 * @param start1 线段 1 起点
 * @param end1   线段 1 终点
 * @param start2 线段 2 起点
 * @param end2   线段 2 终点
 * @return 两个线段最近距离的平方
 */
fun getClosestDistanceBetweenSegmentsSqr(
    start1: Vector3d,
    end1: Vector3d,
    start2: Vector3d,
    end2: Vector3d
): Double {
    val u = Vector3d(end1).sub(start1)
    val v = Vector3d(end2).sub(start2)
    val w = Vector3d(start1).sub(start2)
    val a = u.dot(u) // u*u
    val b = u.dot(v) // u*v
    val c = v.dot(v) // v*v
    val d = u.dot(w) // u*w
    val e = v.dot(w) // v*w
    val dt = a * c - b * b
    var sd = dt
    var td = dt
    var sn: Double // sn = be-cd
    var tn: Double // tn = ae-bd
    if (abs(dt - 0) < 1e-6) {
        // 两直线平行
        sn = 0.0 // 在 s 上指定取 s0
        sd = 1.0 // 防止计算时除 0 错误
        tn = e // 按 (公式3) 求 tc
        td = c
    } else {
        sn = (b * e - c * d)
        tn = (a * e - b * d)
        if (sn < 0) {
            // 最近点在 s 起点以外，同平行条件
            sn = 0.0
            tn = e
            td = c
        } else if (sn > sd) {
            // 最近点在 s 终点以外( 即 sc > 1 , 则取 sc = 1 )
            sn = sd
            tn = e + b // 按 (公式3) 计算
            td = c
        }
    }
    if (tn < 0.0) {
        // 最近点在t起点以外
        tn = 0.0
        if (-d < 0)  // 按 (公式2) 计算，如果等号右边小于 0 ，则 sc 也小于零，取 sc=0
            sn = 0.0
        else if (-d > a)  // 按 (公式2) 计算，如果 sc 大于 1 ，取 sc=1
            sn = sd
        else {
            sn = -d
            sd = a
        }
    } else if (tn > td) {
        tn = td
        if ((-d + b) < 0.0) sn = 0.0
        else if ((-d + b) > a) sn = sd
        else {
            sn = (-d + b)
            sd = a
        }
    }
    val sc: Double = if (abs(sn - 0) < 1e-6) 0.0 else sn / sd
    val tc: Double = if (abs(tn - 0) < 1e-6) 0.0 else tn / td
    val dP = Vector3d(w).add(u.mul(sc)).sub(v.mul(tc))
    return dP.dot(dP)
}

/**
 * 计算OBB上离待判定点最近的点
 *
 * @param point 待判定点
 * @param obb   OBB盒
 * @return 在OBB上离待判定点最近的点
 */
fun <T: ITargetLocation<*>> getClosestPointOBB(point: Vector3d, obb: IOBB<T>): Vector3d {
    val nearP = Vector3d(obb.center)
    // 求球心与 OBB 中心的距离向量 从 OBB 中心指向球心
    val dist = point.sub(nearP, Vector3d())
    val extents = doubleArrayOf(obb.halfExtents.x, obb.halfExtents.y, obb.halfExtents.z)
    val axes = obb.axes
    for (i in 0..2) {
        // 计算距离向量到 OBB 坐标轴的投影长度 即距离向量在 OBB 坐标系中的对应坐标轴的长度
        var distance = dist.dot(axes[i])
        distance = clamp(distance, -extents[i], extents[i])
        // 还原到世界坐标
        nearP.x += distance * axes[i].x
        nearP.y += distance * axes[i].y
        nearP.z += distance * axes[i].z
    }
    return nearP
}

private fun <T: ITargetLocation<*>> getClosestPointAABB(point: Vector3d, aabb: IAABB<T>): Vector3d {
    val nearP = Vector3d()
    val min = aabb.min
    val max = aabb.max
    nearP.x = clamp(point.x, min.x, max.x)
    nearP.y = clamp(point.y, min.y, max.y)
    nearP.z = clamp(point.z, min.z, max.z)
    return nearP
}

fun ITargetLocation<*>.coordinateConverter(): ICoordinateConverter {
    return TargetCoordinateConverter(this)
}
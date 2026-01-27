package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * AABB 碰撞箱接口。
 *
 * 区别于原版实现，使用中心点加半长宽高定义。
 *
 * @param T 目标位置类型
 * @property halfExtents 轴半长
 * @property center 中心点
 * @property min 最小点
 * @property max 最大点
 */
interface IAABB<T: ITargetLocation<*>> : ICollider<T> {

    var halfExtents: Vector3d

    var center: Vector3d

    val min: Vector3d

    val max: Vector3d

    override val type: ColliderType
        get() = ColliderType.AABB
}

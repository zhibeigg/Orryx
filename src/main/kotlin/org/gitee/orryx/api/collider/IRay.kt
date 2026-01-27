package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 射线碰撞箱接口。
 *
 * @param T 目标位置类型
 * @property length 长度
 * @property origin 起点
 * @property end 终点
 * @property direction 方向
 */
interface IRay<T: ITargetLocation<*>> : ICollider<T> {

    var length: Double

    val origin: Vector3d

    val end: Vector3d

    var direction: Vector3d

    override val type: ColliderType
        get() = ColliderType.RAY
}

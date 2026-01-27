package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 胶囊体碰撞箱接口。
 *
 * 性能优于 OBB 碰撞箱。
 *
 * @param T 目标位置类型
 * @property height 高度
 * @property radius 半径
 * @property center 中心点
 * @property rotation 旋转
 * @property direction 方向向量
 */
interface ICapsule<T: ITargetLocation<*>> : ICollider<T> {

    var height: Double

    var radius: Double

    var center: Vector3d

    var rotation: Quaterniond

    val direction: Vector3d

    override val type: ColliderType
        get() = ColliderType.CAPSULE
}

package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 球体碰撞箱接口。
 *
 * @param T 目标位置类型
 * @property radius 半径
 * @property center 中心点
 */
interface ISphere<T: ITargetLocation<*>> : ICollider<T> {

    var radius: Double

    var center: Vector3d

    override val type: ColliderType
        get() = ColliderType.SPHERE
}

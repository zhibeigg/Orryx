package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 球体碰撞箱
 */
interface ISphere<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 半径
     *
     * 设置半径
     * */
    var radius: Double

    /**
     * 中心点
     *
     * 设置中心点
     * */
    var center: Vector3d

    override val type: ColliderType
        get() = ColliderType.SPHERE
}
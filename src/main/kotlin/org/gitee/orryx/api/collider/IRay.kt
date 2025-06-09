package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/**
 * 射线碰撞箱
 * */
interface IRay<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 长度
     *
     * 设置长度
     * */
    var length: Double

    /**
     * 起点
     * */
    val origin: Vector3d

    /**
     * 终点
     * */
    val end: Vector3d

    /**
     * 方向
     *
     * 设置方向
     * */
    var direction: Vector3d

    override val type: ColliderType
        get() = ColliderType.RAY
}

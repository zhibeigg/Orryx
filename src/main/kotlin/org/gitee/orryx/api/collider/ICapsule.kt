package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * 胶囊体碰撞箱
 *
 * 性能优于OBB碰撞箱
 */
interface ICapsule<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 设置高度
     * @return 高度
     */
    var height: Double

    /**
     * 设置半径
     * @return 半径
     * */
    var radius: Double

    /**
     * 设置中心点
     * @return 中心点
     * */
    var center: Vector3d

    /**
     * 设置旋转
     * @return 旋转
     */
    var rotation: Quaterniond

    /**
     * @return 方向向量
     */
    val direction: Vector3d

    override val type: ColliderType
        get() = ColliderType.CAPSULE
}
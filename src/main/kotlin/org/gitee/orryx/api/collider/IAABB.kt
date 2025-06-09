package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Vector3d

/** AABB碰撞箱
 *
 * 区别于原版实现，使用中心点加半长宽高定义。 */
interface IAABB<T: ITargetLocation<*>> : ICollider<T> {

    /**
     * 设置轴半长
     *
     * @return 轴半长
     * */
    var halfExtents: Vector3d

    /**
     * 设置中心点
     *
     * @return 中心点w
     */
    var center: Vector3d

    /**
     * @return 最小点
     */
    val min: Vector3d

    /**
     * @return 最大点
     */
    val max: Vector3d

    override val type: ColliderType
        get() = ColliderType.AABB
}
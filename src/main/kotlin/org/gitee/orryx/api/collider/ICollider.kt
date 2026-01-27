package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation

/**
 * 碰撞箱接口。
 *
 * 依附于 [ITargetLocation]。
 *
 * @param T 目标位置类型
 * @property type 碰撞箱类型
 * @property fastCollider 快速 AABB 碰撞箱，为 null 时不进行快速排除
 */
interface ICollider<T: ITargetLocation<*>> {

    val type: ColliderType

    val fastCollider: IAABB<T>?
        get() = null

    /**
     * 设置是否禁用碰撞箱。
     *
     * @param disable 是否禁用
     */
    fun setDisable(disable: Boolean)

    /**
     * 是否被禁用。
     *
     * @return 是否禁用
     */
    fun disable(): Boolean
}

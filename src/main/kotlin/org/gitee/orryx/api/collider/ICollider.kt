package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation

/** 碰撞箱接口
 *
 * 依附与 ITargetLocation 上
 *
 * ```
 * ICollider<ITarget>
 * ```
 * */
interface ICollider<T: ITargetLocation<*>> {

    /**
     * @return 碰撞箱类型
     */
    val type: ColliderType

    /** 通过AABB碰撞箱进行快速排除，提高碰撞检测效率。
     *
     * AABB要求：
     * - 完整涵盖整个碰撞箱
     * - 尽可能小
     *
     * 可为Null，则不进行快速排除。
     *
     * @return 快速碰撞箱
     */
    val fastCollider: IAABB<T>?
        get() = null

    /**
     * @param disable 设置禁用
     */
    fun setDisable(disable: Boolean)

    /**
     * @return 是否禁用
     */
    fun disable(): Boolean
}
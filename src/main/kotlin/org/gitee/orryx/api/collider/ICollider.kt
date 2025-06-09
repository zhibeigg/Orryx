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
     * 当与其他碰撞体发生碰撞时调用
     */
    val onCollideFunction: ICollideFunction

    /**
     * @return 碰撞箱类型
     */
    val type: ColliderType

    val fastCollider: IAABB<T>?

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

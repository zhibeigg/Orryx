package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation

/**
 * 复合碰撞箱接口。
 *
 * 组合多个碰撞箱为一个整体，可嵌套。
 *
 * @param T 目标位置类型
 * @param C 子碰撞箱类型
 * @property collidersCount 碰撞箱数量
 */
interface IComposite<T: ITargetLocation<*>, C : ICollider<T>> : ICollider<T> {

    val collidersCount: Int

    /**
     * 获取指定索引的碰撞箱。
     *
     * @param index 碰撞箱索引
     * @return 碰撞箱实例
     */
    fun getCollider(index: Int): C

    /**
     * 设置指定索引的碰撞箱。
     *
     * @param index 碰撞箱索引
     * @param collider 碰撞箱实例
     */
    fun setCollider(index: Int, collider: C)

    /**
     * 添加碰撞箱。
     *
     * @param collider 碰撞箱实例
     */
    fun addCollider(collider: C)

    /**
     * 移除指定索引的碰撞箱。
     *
     * @param index 碰撞箱索引
     */
    fun removeCollider(index: Int)

    override val type: ColliderType
        get() = ColliderType.COMPOSITE
}

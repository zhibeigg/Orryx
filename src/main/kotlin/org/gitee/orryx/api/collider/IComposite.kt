package org.gitee.orryx.api.collider

import org.gitee.orryx.core.targets.ITargetLocation

/**
 * 复合碰撞箱
 *
 * 组合多个碰撞箱为一个整体，可嵌套
 * */
interface IComposite<T: ITargetLocation<*>, C : ICollider<T>> : ICollider<T> {

    /**
     * 碰撞箱数量
     * */
    val collidersCount: Int

    /**
     * 获取碰撞箱
     * */
    fun getCollider(index: Int): C

    /**
     * 设置指定索引碰撞箱
     * */
    fun setCollider(index: Int, collider: C)

    /**
     * 添加碰撞箱
     * */
    fun addCollider(collider: C)

    /**
     * 移除指定索引碰撞箱
     * */
    fun removeCollider(index: Int)

    override val type: ColliderType
        get() = ColliderType.COMPOSITE
}
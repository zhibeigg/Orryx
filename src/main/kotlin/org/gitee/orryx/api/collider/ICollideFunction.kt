package org.gitee.orryx.api.collider

@FunctionalInterface
interface ICollideFunction {

    /**
     * 当与其他碰撞体发生碰撞时调用
     *
     * @param entity 发生碰撞的实体
     * @param otherEntity 与其发生碰撞的实体
     * @param other 发生碰撞的另一个碰撞箱
     */
    fun <O, T> run(entity: T, otherEntity: O, other: ICollider<*>)
}
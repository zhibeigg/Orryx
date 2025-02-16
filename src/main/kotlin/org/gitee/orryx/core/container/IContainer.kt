package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget

interface IContainer {

    val targets: MutableSet<ITarget<*>>

    /**
     * 合并两个容器
     * @param other 被合并容器
     * @return 原容器
     * */
    fun merge(other: IContainer): IContainer

    /**
     * 合并两个容器
     * @param other 被合并容器
     * @return 原容器
     * */
    infix fun and(other: IContainer): IContainer

    /**
     * 移除原容器中包含[other]容器的目标
     * @param other 对照容器
     * @return 原容器
     * */
    fun remove(other: IContainer): IContainer

    /**
     * 移除容器中的目标
     * @param target 目标
     * @return 原容器
     * */
    fun remove(target: ITarget<*>): IContainer

    /**
     * 向容器中添加目标
     * @param target 目标
     * @return 原容器
     * */
    fun add(target: ITarget<*>): IContainer

    /**
     * 循环容器中的每个目标
     *
     * 如果[predicate]返回 true 则删除目标
     * @param predicate 对目标执行的匿名方法
     * @return 原容器
     * */
    fun removeIf(predicate: (target: ITarget<*>) -> Boolean): IContainer

    /**
     * 循环容器[other]中的每个目标
     *
     * 如果[predicate]返回 true 则合并目标
     * @param other 被合并容器
     * @param predicate 对目标执行的匿名方法
     * @return 原容器
     * */
    fun mergeIf(other: IContainer, predicate: (target: ITarget<*>) -> Boolean): IContainer

    /**
     * 循环执行容器中每个目标的匿名方法
     * @param action 对目标执行的匿名方法
     * */
    fun foreach(action: (target: ITarget<*>) -> Unit)

    /**
     * 获取第一个
     * @throws NoSuchElementException 如果容器为空
     * */
    fun first(): ITarget<*>

    /**
     * 获取第一个或null
     * */
    fun firstOrNull(): ITarget<*>?

    /**
     * 添加全部目标到容器
     * @param targets 添加的所有目标
     * @return 原容器
     * */
    fun addAll(targets: Iterable<ITarget<*>>): IContainer

    /**
     * 返回前几个目标
     * @param amount 数量
     * @return 目标集合
     * */
    fun take(amount: Int): List<ITarget<*>>

    /**
     * 返回跳过几个目标剩下的目标
     * @param amount 数量
     * @return 目标集合
     * */
    fun drop(amount: Int): List<ITarget<*>>

    /**
     * 克隆一个容器
     * @return 新容器
     * */
    fun clone(): IContainer

}
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
     * 如果[func]返回 true 则删除目标
     * @param func 对目标执行的匿名方法
     * @return 原容器
     * */
    fun removeIf(func: (target: ITarget<*>) -> Boolean): IContainer

    /**
     * 循环容器[other]中的每个目标
     *
     * 如果[func]返回 true 则合并目标
     * @param other 被合并容器
     * @param func 对目标执行的匿名方法
     * @return 原容器
     * */
    fun mergeIf(other: IContainer, func: (target: ITarget<*>) -> Boolean): IContainer

    /**
     * 循环执行容器中每个目标的匿名方法
     * @param func 对目标执行的匿名方法
     * */
    fun foreach(func: (target: ITarget<*>) -> Unit)

    /**
     * 添加全部目标到容器
     * @param targets 添加的所有目标
     * @return 原容器
     * */
    fun addAll(targets: Iterable<ITarget<*>>): IContainer

    /**
     * 克隆一个容器
     * @return 新容器
     * */
    fun clone(): IContainer

}
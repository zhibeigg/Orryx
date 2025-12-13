package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget

/**
 * 目标消费者函数式接口。
 *
 * 此接口用于替代 Kotlin 的 `(ITarget<*>) -> Unit` 函数类型，
 * 以避免其他插件依赖时因 Kotlin 重定向导致的兼容性问题。
 *
 * @see IContainer
 * @see ITarget
 */
@FunctionalInterface
fun interface TargetConsumer {

    /**
     * 消费目标
     *
     * @param target 目标
     */
    fun accept(target: ITarget<*>)
}

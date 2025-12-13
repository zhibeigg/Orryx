package org.gitee.orryx.core.container

import org.gitee.orryx.core.targets.ITarget

/**
 * 目标断言函数式接口。
 *
 * 此接口用于替代 Kotlin 的 `(ITarget<*>) -> Boolean` 函数类型，
 * 以避免其他插件依赖时因 Kotlin 重定向导致的兼容性问题。
 *
 * @see IContainer
 * @see ITarget
 */
@FunctionalInterface
fun interface TargetPredicate {

    /**
     * 测试目标是否满足条件
     *
     * @param target 目标
     * @return 是否满足条件
     */
    fun test(target: ITarget<*>): Boolean
}

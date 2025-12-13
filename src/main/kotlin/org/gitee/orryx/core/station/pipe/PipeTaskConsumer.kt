package org.gitee.orryx.core.station.pipe

/**
 * 管式任务消费者函数式接口。
 *
 * 此接口用于替代 Kotlin 的 `(IPipeTask) -> Unit` 函数类型，
 * 以避免其他插件依赖时因 Kotlin 重定向导致的兼容性问题。
 *
 * @see IPipeTask
 * @see IPipePeriodTask
 */
@FunctionalInterface
fun interface PipeTaskConsumer {

    /**
     * 消费管式任务
     *
     * @param task 管式任务
     */
    fun accept(task: IPipeTask)
}

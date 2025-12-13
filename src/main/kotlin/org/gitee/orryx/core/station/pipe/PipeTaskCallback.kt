package org.gitee.orryx.core.station.pipe

import java.util.concurrent.CompletableFuture

/**
 * 管式任务回调函数式接口。
 *
 * 此接口用于替代 Kotlin 的 `(IPipeTask) -> CompletableFuture<Any?>` 函数类型，
 * 以避免其他插件依赖时因 Kotlin 重定向导致的兼容性问题。
 *
 * @see IPipeTask
 */
@FunctionalInterface
fun interface PipeTaskCallback {

    /**
     * 执行回调
     *
     * @param task 管式任务
     * @return 异步结果
     */
    fun invoke(task: IPipeTask): CompletableFuture<Any?>
}

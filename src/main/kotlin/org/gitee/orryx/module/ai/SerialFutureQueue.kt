package org.gitee.orryx.module.ai

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 按 key 串行执行异步操作。
 *
 * 对外结果被取消或超时不会提前移除内部 tail；该节点仍会等待前序结束后再跳过，避免后续请求越过它并发执行。
 */
internal class SerialFutureQueue<K> {

    private val tails = ConcurrentHashMap<K, CompletableFuture<Unit>>()

    fun <T> enqueue(
        key: K,
        result: CompletableFuture<T>,
        operation: () -> CompletableFuture<Unit>,
    ): CompletableFuture<Unit> {
        lateinit var node: CompletableFuture<Unit>
        tails.compute(key) { _, previous ->
            val ready = previous?.handle { _, _ -> Unit } ?: CompletableFuture.completedFuture(Unit)
            node = ready.thenCompose {
                if (result.isDone) {
                    CompletableFuture.completedFuture(Unit)
                } else {
                    try {
                        operation()
                    } catch (throwable: Throwable) {
                        failedFuture(throwable)
                    }
                }
            }.handle { _, throwable ->
                if (throwable != null && !result.isDone) {
                    result.completeExceptionally(unwrapCompletionFailure(throwable))
                }
                Unit
            }
            node
        }
        node.whenComplete { _, _ -> tails.remove(key, node) }
        return node
    }

    internal fun hasPending(key: K): Boolean = tails.containsKey(key)

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    private fun unwrapCompletionFailure(throwable: Throwable): Throwable {
        return throwable.cause ?: throwable
    }
}

internal fun <T> appendBoundedConversation(
    history: List<T>,
    message: T,
    maxMessages: Int,
    isSystem: (T) -> Boolean,
): List<T> {
    require(maxMessages > 0) { "maxMessages 必须大于 0" }
    val system = history.firstOrNull(isSystem)
    val recent = history.filterNot { it === system }.takeLast(maxMessages - 1)
    val combined = buildList {
        system?.let(::add)
        addAll(recent)
        add(message)
    }
    if (combined.size <= maxMessages) return combined
    val tailSize = maxMessages - if (system == null) 0 else 1
    return buildList {
        system?.let(::add)
        addAll(combined.filterNot { it === system }.takeLast(tailSize))
    }
}

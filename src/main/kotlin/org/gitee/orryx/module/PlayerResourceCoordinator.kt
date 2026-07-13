package org.gitee.orryx.module

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * 同一玩家的 PlayerProfile 写事务共用一条串行 lane。
 *
 * 事务覆盖 Mana/Spirit、Flag、点数、职业切换和通用保存的读取、事件、
 * 内存修改、持久化与失败回滚，避免旧 Profile 快照覆盖较新的字段。
 */
internal object PlayerResourceCoordinator {

    private val lock = Any()
    private val tails = HashMap<UUID, CompletableFuture<Unit>>()
    private var accepting = true
    private var firstFailure: Throwable? = null

    fun <T> enqueue(player: UUID, operation: () -> CompletableFuture<T>): CompletableFuture<T> {
        val result: CompletableFuture<T>
        val tail: CompletableFuture<Unit>
        synchronized(lock) {
            if (!accepting) {
                return failed(IllegalStateException("Orryx 玩家资源事务队列正在关闭"))
            }
            val ready = tails[player]?.handle { _, _ -> Unit }
                ?: CompletableFuture.completedFuture(Unit)
            result = ready.thenCompose {
                try {
                    operation()
                } catch (throwable: Throwable) {
                    failed(throwable)
                }
            }
            tail = result.handle { _, throwable ->
                if (throwable != null) synchronized(lock) {
                    if (firstFailure == null) firstFailure = unwrap(throwable)
                }
                Unit
            }
            tails[player] = tail
        }
        tail.whenComplete { _, _ ->
            synchronized(lock) {
                if (tails[player] === tail) tails.remove(player)
            }
        }
        return result
    }

    fun shutdown(): CompletableFuture<Unit> {
        val pending: Array<CompletableFuture<Unit>>
        synchronized(lock) {
            accepting = false
            pending = tails.values.toTypedArray()
        }
        return CompletableFuture.allOf(*pending).thenCompose {
            val failure = synchronized(lock) { firstFailure }
            if (failure == null) CompletableFuture.completedFuture(Unit) else failed(failure)
        }
    }

    private fun unwrap(throwable: Throwable): Throwable {
        var current = throwable
        while (current is CompletionException && current.cause != null) {
            current = current.cause ?: break
        }
        return current
    }

    private fun <T> failed(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }
}

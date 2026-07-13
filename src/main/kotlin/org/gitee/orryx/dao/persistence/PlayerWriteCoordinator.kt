package org.gitee.orryx.dao.persistence

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 按玩家隔离的非阻塞写通道。
 *
 * 同一玩家的写入严格串行；尚未开始的同键写入只保留最新快照，
 * 被合并请求的 Future 会在最新快照落库后一起完成。
 */
internal object PlayerWriteCoordinator {

    private class PendingWrite(
        var operation: () -> CompletableFuture<Unit>,
        val waiters: MutableList<CompletableFuture<Unit>>
    )

    private data class EnqueuedWrite(
        val future: CompletableFuture<Unit>,
        val shouldStartDrain: Boolean,
    )

    private class PlayerLane {
        private val lock = Any()
        private val pending = LinkedHashMap<String, PendingWrite>()
        private val idleWaiters = mutableListOf<CompletableFuture<Unit>>()
        private var running = false
        private var firstFailure: Throwable? = null

        fun enqueue(key: String, operation: () -> CompletableFuture<Unit>): EnqueuedWrite {
            val result = CompletableFuture<Unit>()
            var startDrain = false
            synchronized(lock) {
                val existing = pending.remove(key)
                if (existing == null) {
                    pending[key] = PendingWrite(operation, mutableListOf(result))
                } else {
                    existing.operation = operation
                    existing.waiters += result
                    // 重新放到队尾，保持“最新一次写入”的全局顺序。
                    pending[key] = existing
                }
                if (!running) {
                    running = true
                    startDrain = true
                }
            }
            return EnqueuedWrite(result, startDrain)
        }

        fun startDrain() {
            drainNext()
        }

        fun idleFuture(): CompletableFuture<Unit> {
            synchronized(lock) {
                if (!running && pending.isEmpty()) {
                    val failure = firstFailure
                    firstFailure = null
                    return if (failure == null) {
                        CompletableFuture.completedFuture(Unit)
                    } else {
                        CompletableFuture<Unit>().also { it.completeExceptionally(failure) }
                    }
                }
                return CompletableFuture<Unit>().also { idleWaiters += it }
            }
        }

        fun isIdle(): Boolean {
            return synchronized(lock) { !running && pending.isEmpty() }
        }

        private fun drainNext() {
            var write: PendingWrite? = null
            var completedIdleWaiters: List<CompletableFuture<Unit>> = emptyList()
            var idleFailure: Throwable? = null
            synchronized(lock) {
                val first = pending.entries.firstOrNull()
                if (first == null) {
                    running = false
                    completedIdleWaiters = idleWaiters.toList()
                    idleWaiters.clear()
                    if (completedIdleWaiters.isNotEmpty()) {
                        idleFailure = firstFailure
                        firstFailure = null
                    }
                } else {
                    pending.remove(first.key)
                    write = first.value
                }
            }

            val nextWrite = write
            if (nextWrite == null) {
                // 不在 lane 锁内完成 Future，避免同步回调造成锁顺序反转。
                completedIdleWaiters.forEach { waiter ->
                    if (idleFailure == null) waiter.complete(Unit) else waiter.completeExceptionally(idleFailure)
                }
                return
            }

            val future = try {
                nextWrite.operation()
            } catch (throwable: Throwable) {
                CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
            }
            future.whenComplete { _, throwable ->
                if (throwable != null) {
                    synchronized(lock) {
                        if (firstFailure == null) firstFailure = throwable
                    }
                }
                nextWrite.waiters.forEach { waiter ->
                    if (throwable == null) {
                        waiter.complete(Unit)
                    } else {
                        waiter.completeExceptionally(throwable)
                    }
                }
                drainNext()
            }
        }
    }

    private val stateLock = Any()
    private val lanes = ConcurrentHashMap<UUID, PlayerLane>()

    @Volatile
    private var accepting = true

    fun enqueue(player: UUID, key: String, operation: () -> CompletableFuture<Unit>): CompletableFuture<Unit> {
        val lane: PlayerLane
        val enqueued: EnqueuedWrite
        synchronized(stateLock) {
            if (!accepting) {
                return CompletableFuture<Unit>().also {
                    it.completeExceptionally(IllegalStateException("Orryx 持久化服务正在关闭"))
                }
            }
            lane = lanes.computeIfAbsent(player) { PlayerLane() }
            enqueued = lane.enqueue(key, operation)
        }
        if (enqueued.shouldStartDrain) {
            lane.startDrain()
        }
        return enqueued.future
    }

    fun flush(player: UUID): CompletableFuture<Unit> {
        return synchronized(stateLock) {
            lanes[player]?.idleFuture() ?: CompletableFuture.completedFuture(Unit)
        }
    }

    fun release(player: UUID): CompletableFuture<Unit> {
        val lane: PlayerLane
        val idle: CompletableFuture<Unit>
        synchronized(stateLock) {
            lane = lanes[player] ?: return CompletableFuture.completedFuture(Unit)
            idle = lane.idleFuture()
        }
        return idle.thenApply {
            synchronized(stateLock) {
                if (lane.isIdle()) {
                    lanes.remove(player, lane)
                }
            }
            Unit
        }
    }

    fun shutdown(): CompletableFuture<Unit> {
        val futures = synchronized(stateLock) {
            accepting = false
            lanes.values.map { it.idleFuture() }
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            synchronized(stateLock) {
                lanes.clear()
            }
            Unit
        }
    }
}

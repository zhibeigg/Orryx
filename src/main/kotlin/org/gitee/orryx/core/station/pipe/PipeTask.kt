package org.gitee.orryx.core.station.pipe

import org.gitee.orryx.utils.composeOnMainThread
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.ScriptContext
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

/** 可被事件打断或在超时后完成的管道任务。 */
class PipeTask(
    override val uuid: UUID,
    override val scriptContext: ScriptContext?,
    override val brokeTriggers: Set<String>,
    override val timeout: Long,
    override val onBrock: PipeTaskCallback,
    override val onComplete: PipeTaskCallback,
    override val periodTask: IPipePeriodTask?,
    autoStart: Boolean = true,
    private val callbackExecutor: ((() -> CompletableFuture<Any?>) -> CompletableFuture<Any?>) = { block ->
        composeOnMainThread(block)
    },
) : IPipeTask {

    private enum class State { CREATED, OPEN, BREAKING, COMPLETING, FAILED, CLOSED }

    companion object {
        fun builder(): PipeBuilder = PipeBuilder()
    }

    override val startStamp: Long = System.currentTimeMillis()
    override val result: CompletableFuture<Any?> = CompletableFuture()

    private val completedTask = AtomicReference<PlatformExecutor.PlatformTask?>()
    private val state = AtomicReference(State.CREATED)

    internal val isClosed: Boolean
        get() = state.get() !in setOf(State.CREATED, State.OPEN)

    init {
        result.whenComplete { _, _ ->
            if (result.isCancelled) broke()
        }
        if (autoStart) start()
    }

    internal fun start(): PipeTask {
        if (!state.compareAndSet(State.CREATED, State.OPEN)) return this
        try {
            PipeTaskManager.addPipeTask(this)
            periodTask?.start(this)
            val task = submit(delay = timeout) { complete() }
            if (!completedTask.compareAndSet(null, task) || state.get() != State.OPEN) {
                task.cancel()
            }
        } catch (throwable: Throwable) {
            if (state.compareAndSet(State.OPEN, State.FAILED)) {
                cleanup()?.let { throwable.addSuppressed(it) }
                state.set(State.CLOSED)
                result.completeExceptionally(throwable)
            }
            throw throwable
        }
        return this
    }

    override fun broke(): CompletableFuture<Any?> {
        return claimAndClose(State.BREAKING, onBrock)
    }

    override fun complete(): CompletableFuture<Any?> {
        return claimAndClose(State.COMPLETING, onComplete)
    }

    internal fun breakFromTrigger(onClaim: () -> Unit): CompletableFuture<Any?>? {
        while (true) {
            val current = state.get()
            if (current !in setOf(State.CREATED, State.OPEN)) return null
            if (state.compareAndSet(current, State.BREAKING)) return closeClaimed(onBrock, onClaim)
        }
    }

    internal fun fail(throwable: Throwable): CompletableFuture<Any?> {
        while (true) {
            val current = state.get()
            if (current !in setOf(State.CREATED, State.OPEN)) return result
            if (state.compareAndSet(current, State.FAILED)) {
                return closeClaimed(PipeTaskCallback { failedFuture(throwable) })
            }
        }
    }

    internal fun close(func: PipeTaskCallback): CompletableFuture<Any?> {
        return claimAndClose(State.BREAKING, func)
    }

    /** 脚本上下文清理时仅释放资源，不再执行任何业务终止回调。 */
    internal fun abort(): CompletableFuture<Any?> {
        return claimAndClose(State.FAILED, PipeTaskCallback { CompletableFuture.completedFuture(null) })
    }

    private fun claimAndClose(target: State, callback: PipeTaskCallback): CompletableFuture<Any?> {
        while (true) {
            val current = state.get()
            if (current !in setOf(State.CREATED, State.OPEN)) return result
            if (state.compareAndSet(current, target)) return closeClaimed(callback)
        }
    }

    private fun closeClaimed(
        callback: PipeTaskCallback,
        onClaim: () -> Unit = {},
    ): CompletableFuture<Any?> {
        val cleanupFailure = cleanup()
        val callbackFuture = try {
            callbackExecutor {
                onClaim()
                try {
                    callback.invoke(this)
                } catch (throwable: Throwable) {
                    failedFuture(throwable)
                }
            }
        } catch (throwable: Throwable) {
            failedFuture(throwable)
        }
        callbackFuture.whenComplete { value, callbackFailure ->
            state.set(State.CLOSED)
            val failure = combine(cleanupFailure, callbackFailure)
            if (failure == null) result.complete(value) else result.completeExceptionally(failure)
        }
        return result
    }

    private fun cleanup(): Throwable? {
        var failure: Throwable? = null
        try {
            completedTask.getAndSet(null)?.cancel()
        } catch (throwable: Throwable) {
            failure = throwable
        }
        try {
            periodTask?.cancel(this)
        } catch (throwable: Throwable) {
            if (failure == null) failure = throwable else if (failure !== throwable) failure.addSuppressed(throwable)
        }
        try {
            PipeTaskManager.removePipeTask(this)
        } catch (throwable: Throwable) {
            if (failure == null) failure = throwable else if (failure !== throwable) failure.addSuppressed(throwable)
        }
        return failure
    }

    private fun combine(first: Throwable?, second: Throwable?): Throwable? {
        val primary = first ?: second ?: return null
        if (second != null && second !== primary) primary.addSuppressed(second)
        return primary
    }

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }
}

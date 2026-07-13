package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 可被事件打断或在超时后完成的管道任务。
 */
class PipeTask(
    override val uuid: UUID,
    override val scriptContext: ScriptContext?,
    override val brokeTriggers: Set<String>,
    override val timeout: Long,
    override val onBrock: PipeTaskCallback,
    override val onComplete: PipeTaskCallback,
    override val periodTask: IPipePeriodTask?
) : IPipeTask {

    companion object {

        fun builder(): PipeBuilder = PipeBuilder()
    }

    override val startStamp: Long = System.currentTimeMillis()
    override val result: CompletableFuture<Any?> = CompletableFuture()

    private val completedTask = AtomicReference<PlatformExecutor.PlatformTask?>()
    private val closed = AtomicBoolean(false)

    internal val isClosed: Boolean
        get() = closed.get()

    init {
        PipeTaskManager.addPipeTask(this)
        try {
            periodTask?.start(this)
            val task = submit(delay = timeout) {
                complete()
            }
            if (!completedTask.compareAndSet(null, task) || closed.get()) {
                task.cancel()
            }
        } catch (ex: Throwable) {
            if (closed.compareAndSet(false, true)) {
                periodTask?.cancel(this)
                PipeTaskManager.removePipeTask(this)
            }
            throw ex
        }
    }

    override fun broke(): CompletableFuture<Any?> {
        return close(onBrock)
    }

    override fun complete(): CompletableFuture<Any?> {
        return close(onComplete)
    }

    internal fun close(func: PipeTaskCallback): CompletableFuture<Any?> {
        if (!closed.compareAndSet(false, true)) return result

        completedTask.getAndSet(null)?.cancel()
        periodTask?.cancel(this)
        PipeTaskManager.removePipeTask(this)

        val callback = try {
            func.invoke(this)
        } catch (ex: Throwable) {
            CompletableFuture<Any?>().also { it.completeExceptionally(ex) }
        }
        callback.whenComplete { value, throwable ->
            if (throwable == null) {
                result.complete(value)
            } else {
                result.completeExceptionally(throwable)
            }
        }
        return result
    }
}
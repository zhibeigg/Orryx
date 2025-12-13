package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

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

    private var completedTask: PlatformExecutor.PlatformTask

    private val closed = AtomicBoolean(false)
    private val taskLock = ReentrantLock()

    init {
        taskLock.lock()
        try {
            completedTask = submit(delay = timeout) {
                complete()
            }
            periodTask?.start(this)
            PipeTaskManager.addPipeTask(this)
        } finally {
            taskLock.unlock()
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

        taskLock.lock()
        try {
            completedTask.cancel()
            periodTask?.cancel(this)
            PipeTaskManager.removePipeTask(this)
            return func.invoke(this).whenComplete { _, _ ->
                result.complete(null)
            }
        } finally {
            taskLock.unlock()
        }
    }
}
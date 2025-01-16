package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

class PipeTask(
    override val uuid: UUID,
    override val scriptContext: ScriptContext?,
    override val brokeTriggers: Set<String>,
    override val timeout: Long,
    override val onBrock: (IPipeTask) -> CompletableFuture<Any?>,
    override val onComplete: (IPipeTask) -> CompletableFuture<Any?>,
    override val periodTask: IPipePeriodTask?
): IPipeTask {

    override val startStamp: Long = System.currentTimeMillis()

    override val result: CompletableFuture<Any?> = CompletableFuture()

    private var completedTask: PlatformExecutor.PlatformTask

    init {
        completedTask = submit(delay = timeout) {
            complete()
        }
        periodTask?.start(this)
        PipeManager.addPipeTask(this)
    }

    override fun broke(): CompletableFuture<Any?> {
        return close(onBrock)
    }

    override fun complete(): CompletableFuture<Any?> {
        return close(onComplete)
    }

    fun close(func: (IPipeTask) -> CompletableFuture<Any?>): CompletableFuture<Any?> {
        completedTask.cancel()
        periodTask?.cancel(this)
        PipeManager.removePipeTask(this)
        if (result.isDone) return result
        return func(this).thenApply {
            result.complete(it)
        }
    }

}
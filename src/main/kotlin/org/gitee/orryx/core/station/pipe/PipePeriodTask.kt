package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** 周期回调严格串行；上一次未完成时跳过本次 tick。 */
class PipePeriodTask(
    override val period: Long,
    override val onPeriod: PipeTaskConsumer,
    private val asyncPeriod: PipeTaskCallback? = null,
) : IPipePeriodTask {

    constructor(period: Long, asyncPeriod: PipeTaskCallback) : this(
        period,
        PipeTaskConsumer { },
        asyncPeriod,
    )

    private val cancelled = AtomicBoolean(false)
    private val taskRef = AtomicReference<PlatformExecutor.PlatformTask?>()
    private val inFlight = AtomicReference<CompletableFuture<Any?>?>()

    override fun start(pipeTask: IPipeTask) {
        if (cancelled.get()) return
        val task = submit(period = period) {
            val concrete = pipeTask as? PipeTask
            if (cancelled.get() || concrete?.isClosed == true) return@submit

            val placeholder = CompletableFuture<Any?>()
            if (!inFlight.compareAndSet(null, placeholder)) return@submit
            if (cancelled.get() || concrete?.isClosed == true) {
                inFlight.compareAndSet(placeholder, null)
                placeholder.cancel(false)
                return@submit
            }

            val stage = try {
                asyncPeriod?.invoke(pipeTask) ?: run {
                    onPeriod.accept(pipeTask)
                    CompletableFuture.completedFuture<Any?>(null)
                }
            } catch (throwable: Throwable) {
                CompletableFuture<Any?>().also { it.completeExceptionally(throwable) }
            }

            if (cancelled.get() || concrete?.isClosed == true || !inFlight.compareAndSet(placeholder, stage)) {
                stage.cancel(false)
                inFlight.compareAndSet(placeholder, null)
                return@submit
            }

            stage.whenComplete { _, throwable ->
                if (throwable != null) {
                    if (!cancelled.get()) concrete?.fail(throwable) ?: pipeTask.broke()
                    inFlight.compareAndSet(stage, null)
                    return@whenComplete
                }
                if (!cancelled.get() && pipeTask.scriptContext?.breakLoop == true) {
                    pipeTask.scriptContext?.breakLoop = false
                    pipeTask.broke()
                }
                inFlight.compareAndSet(stage, null)
            }
        }
        if (!taskRef.compareAndSet(null, task) || cancelled.get()) {
            task.cancel()
        }
    }

    override fun cancel(pipeTask: IPipeTask) {
        if (cancelled.compareAndSet(false, true)) {
            taskRef.getAndSet(null)?.cancel()
            inFlight.getAndSet(null)?.cancel(false)
        }
    }
}

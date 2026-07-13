package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PipePeriodTask(override val period: Long, override val onPeriod: PipeTaskConsumer): IPipePeriodTask {

    private val cancelled = AtomicBoolean(false)
    private val taskRef = AtomicReference<PlatformExecutor.PlatformTask?>()

    override fun start(pipeTask: IPipeTask) {
        if (cancelled.get()) return
        val task = submit(period = period) {
            if (pipeTask.scriptContext?.breakLoop == true) {
                pipeTask.scriptContext?.breakLoop = false
                pipeTask.broke()
            } else {
                onPeriod.accept(pipeTask)
            }
        }
        if (!taskRef.compareAndSet(null, task) || cancelled.get()) {
            task.cancel()
        }
    }

    override fun cancel(pipeTask: IPipeTask) {
        if (cancelled.compareAndSet(false, true)) {
            taskRef.getAndSet(null)?.cancel()
        }
    }
}
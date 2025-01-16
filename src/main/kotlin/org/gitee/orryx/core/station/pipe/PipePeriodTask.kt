package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor

class PipePeriodTask(override val period: Long, override val onPeriod: (IPipeTask) -> Unit): IPipePeriodTask {

    private lateinit var bukkitRunningTask: PlatformExecutor.PlatformTask

    override fun start(pipeTask: IPipeTask) {
        bukkitRunningTask = submit(period = period) {
            if (pipeTask.scriptContext?.breakLoop == true) {
                pipeTask.broke()
                pipeTask.scriptContext?.breakLoop = false
            } else {
                onPeriod(pipeTask)
            }
        }
    }

    override fun cancel(pipeTask: IPipeTask) {
        bukkitRunningTask.cancel()
    }

}
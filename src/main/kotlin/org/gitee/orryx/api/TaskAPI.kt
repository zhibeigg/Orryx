package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.ITaskAPI
import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.common.task.SimpleTimeoutTask.Companion.register
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.core.station.pipe.PipeTask
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class TaskAPI: ITaskAPI {

    override fun createSimpleTask(tick: Long, closeable: AutoCloseable): SimpleTimeoutTask {
        return SimpleTimeoutTask(tick) { closeable.close() }.register()
    }

    override fun closeSimpleTask(task: SimpleTimeoutTask) {
        SimpleTimeoutTask.cancel(task)
    }

    override fun pipeBuilder(): PipeBuilder {
        return PipeBuilder()
    }

    override fun brokePipeTask(task: PipeTask): CompletableFuture<Any?> {
        return task.broke()
    }

    override fun completePipeTask(task: PipeTask): CompletableFuture<Any?> {
        return task.complete()
    }

    override fun closePipeTask(task: PipeTask, callback: Function<IPipeTask, CompletableFuture<Any?>>): CompletableFuture<Any?> {
        return task.close {
            callback.apply(it)
        }
    }

    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<ITaskAPI>(TaskAPI())
        }
    }
}
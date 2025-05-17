package org.gitee.orryx.api.interfaces

import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.core.station.pipe.PipeTask
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface ITaskAPI {

    fun createSimpleTask(tick: Long, closeable: AutoCloseable): SimpleTimeoutTask

    fun closeSimpleTask(task: SimpleTimeoutTask)

    fun pipeBuilder(): PipeBuilder

    fun completePipeTask(task: PipeTask): CompletableFuture<Any?>

    fun brokePipeTask(task: PipeTask): CompletableFuture<Any?>

    fun closePipeTask(task: PipeTask, callback: Function<IPipeTask, CompletableFuture<Any?>>): CompletableFuture<Any?>
}
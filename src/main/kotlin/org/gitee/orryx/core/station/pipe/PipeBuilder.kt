package org.gitee.orryx.core.station.pipe

import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function

class PipeBuilder {

    private var uuid: UUID = UUID.randomUUID()
    private var scriptContext: ScriptContext? = null
    private var brokeTriggers: Set<String> = emptySet()
    private var timeout: Long? = null
    private var onBrock: (IPipeTask) -> CompletableFuture<Any?> = { CompletableFuture.completedFuture(it) }
    private var onComplete: (IPipeTask) -> CompletableFuture<Any?> = { CompletableFuture.completedFuture(it) }
    private var periodTask: IPipePeriodTask? = null

    fun build(): PipeTask {
        return PipeTask(uuid, scriptContext, brokeTriggers, timeout ?: error("创建PipeTask时未设置timeout"), onBrock, onComplete, periodTask)
    }

    fun uuid(uuid: UUID): PipeBuilder = apply {
        this.uuid = uuid
    }

    fun scriptContext(scriptContext: ScriptContext): PipeBuilder = apply {
        this.scriptContext = scriptContext
    }

    fun brokeTriggers(vararg triggers: String): PipeBuilder = apply {
        brokeTriggers = triggers.toSet()
    }

    fun timeout(timeout: Long): PipeBuilder = apply {
        this.timeout = timeout
    }

    fun onBrock(onBrock: Function<IPipeTask, CompletableFuture<Any?>>): PipeBuilder = apply {
        this.onBrock = { onBrock.apply(it) }
    }

    fun onComplete(onComplete: Function<IPipeTask, CompletableFuture<Any?>>): PipeBuilder = apply {
        this.onComplete = { onComplete.apply(it) }
    }

    fun periodTask(period: Long, func: Consumer<IPipeTask>): PipeBuilder = apply {
        periodTask = PipePeriodTask(period) { func.accept(it) }
    }
}
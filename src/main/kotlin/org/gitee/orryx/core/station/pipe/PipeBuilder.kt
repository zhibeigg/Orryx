package org.gitee.orryx.core.station.pipe

import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

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

    fun uuid(uuid: UUID): PipeBuilder {
        this.uuid = uuid
        return this
    }

    fun scriptContext(scriptContext: ScriptContext): PipeBuilder {
        this.scriptContext = scriptContext
        return this
    }

    fun brokeTriggers(vararg triggers: String): PipeBuilder {
        this.brokeTriggers = triggers.toSet()
        return this
    }

    fun timeout(timeout: Long): PipeBuilder {
        this.timeout = timeout
        return this
    }

    fun onBrock(onBrock: (IPipeTask) -> CompletableFuture<Any?>): PipeBuilder {
        this.onBrock = onBrock
        return this
    }

    fun onComplete(onComplete: (IPipeTask) -> CompletableFuture<Any?>): PipeBuilder {
        this.onComplete = onComplete
        return this
    }

    fun periodTask(period: Long, func: (IPipeTask) -> Unit): PipeBuilder {
        this.periodTask = PipePeriodTask(period, func)
        return this
    }
}
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
    private var onBrock: PipeTaskCallback = PipeTaskCallback { CompletableFuture.completedFuture(it) }
    private var onComplete: PipeTaskCallback = PipeTaskCallback { CompletableFuture.completedFuture(it) }
    private var periodTask: IPipePeriodTask? = null

    /**
     * 构建
     * */
    fun build(): PipeTask {
        return PipeTask(uuid, scriptContext, brokeTriggers, timeout ?: error("创建PipeTask时未设置timeout"), onBrock, onComplete, periodTask)
    }

    fun uuid(uuid: UUID): PipeBuilder = apply {
        this.uuid = uuid
    }

    fun scriptContext(scriptContext: ScriptContext): PipeBuilder = apply {
        this.scriptContext = scriptContext
    }

    /**
     * 打断方式
     * */
    fun brokeTriggers(vararg triggers: String): PipeBuilder = apply {
        brokeTriggers = triggers.toSet()
    }

    /**
     * 完成需要的时间 Tick
     * */
    fun timeout(timeout: Long): PipeBuilder = apply {
        this.timeout = timeout
    }

    /**
     * 打断触发
     * */
    fun onBrock(onBrock: Function<IPipeTask, CompletableFuture<Any?>>): PipeBuilder = apply {
        this.onBrock = PipeTaskCallback { onBrock.apply(it) }
    }

    /**
     * 完成触发
     * */
    fun onComplete(onComplete: Function<IPipeTask, CompletableFuture<Any?>>): PipeBuilder = apply {
        this.onComplete = PipeTaskCallback { onComplete.apply(it) }
    }

    /**
     * 周期触发
     * */
    fun periodTask(period: Long, func: Consumer<IPipeTask>): PipeBuilder = apply {
        periodTask = PipePeriodTask(period) { func.accept(it) }
    }
}
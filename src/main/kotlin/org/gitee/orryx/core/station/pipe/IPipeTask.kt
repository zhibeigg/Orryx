package org.gitee.orryx.core.station.pipe

import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

interface IPipeTask {

    /**
     * 唯一标识
     * */
    val uuid: UUID

    /**
     * 终止Trigger列表
     * */
    val brokeTriggers: Set<String>

    /**
     * 开始时的时间戳
     * */
    val startStamp: Long

    /**
     * 完成时间
     * */
    val timeout: Long

    /**
     * 脚本上下文信息
     * */
    val scriptContext: ScriptContext?

    /**
     * 结果
     * */
    val result: CompletableFuture<Any?>

    /**
     * 当被Trigger终止时执行
     * */
    val onBrock: (IPipeTask) -> CompletableFuture<Any?>

    /**
     * 当Timeout耗尽结束时执行
     * */
    val onComplete: (IPipeTask) -> CompletableFuture<Any?>

    /**
     * 拥有的周期任务
     * */
    val periodTask: IPipePeriodTask?

    /**
     * 中断任务并返回脚本执行内容
     * */
    fun broke(): CompletableFuture<Any?>

    /**
     * 完成任务并返回脚本执行内容
     * */
    fun complete(): CompletableFuture<Any?>

}
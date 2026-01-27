package org.gitee.orryx.core.station.pipe

import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 管道任务接口。
 *
 * @property uuid 唯一标识
 * @property brokeTriggers 终止 Trigger 列表
 * @property startStamp 开始时间戳
 * @property timeout 完成时间
 * @property scriptContext 脚本上下文信息
 * @property result 结果
 * @property onBrock 被 Trigger 终止时执行的回调
 * @property onComplete Timeout 耗尽结束时执行的回调
 * @property periodTask 拥有的周期任务
 */
interface IPipeTask {

    val uuid: UUID

    val brokeTriggers: Set<String>

    val startStamp: Long

    val timeout: Long

    val scriptContext: ScriptContext?

    val result: CompletableFuture<Any?>

    val onBrock: PipeTaskCallback

    val onComplete: PipeTaskCallback

    val periodTask: IPipePeriodTask?

    /**
     * 中断任务并返回脚本执行内容。
     *
     * @return 执行结果
     */
    fun broke(): CompletableFuture<Any?>

    /**
     * 完成任务并返回脚本执行内容。
     *
     * @return 执行结果
     */
    fun complete(): CompletableFuture<Any?>
}

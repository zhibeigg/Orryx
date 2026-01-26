package org.gitee.orryx.api.interfaces

import org.gitee.orryx.core.common.task.SimpleTimeoutTask
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.core.station.pipe.PipeTask
import java.lang.AutoCloseable
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * 任务 API 接口
 *
 * 提供简单超时任务和管道任务的创建与管理功能
 * */
interface ITaskAPI {

    /**
     * 创建简单超时任务
     *
     * 任务会在指定 tick 后自动执行关闭操作
     *
     * @param tick 超时时间（游戏刻）
     * @param closeable 超时后执行的关闭操作
     * @return 创建的任务对象
     * */
    fun createSimpleTask(tick: Long, closeable: AutoCloseable): SimpleTimeoutTask

    /**
     * 关闭简单超时任务
     *
     * 立即取消任务，不会执行关闭操作
     *
     * @param task 要关闭的任务
     * */
    fun closeSimpleTask(task: SimpleTimeoutTask)

    /**
     * 创建管道任务构建器
     *
     * @return 管道任务构建器
     * */
    fun pipeBuilder(): PipeBuilder

    /**
     * 完成管道任务
     *
     * 正常完成任务并执行完成回调
     *
     * @param task 要完成的管道任务
     * @return 完成回调的返回值
     * */
    fun completePipeTask(task: PipeTask): CompletableFuture<Any?>

    /**
     * 中断管道任务
     *
     * 中断任务并执行中断回调
     *
     * @param task 要中断的管道任务
     * @return 中断回调的返回值
     * */
    fun brokePipeTask(task: PipeTask): CompletableFuture<Any?>

    /**
     * 关闭管道任务
     *
     * 使用自定义回调关闭任务
     *
     * @param task 要关闭的管道任务
     * @param callback 关闭时执行的回调函数
     * @return 回调函数的返回值
     * */
    fun closePipeTask(task: PipeTask, callback: Function<IPipeTask, CompletableFuture<Any?>>): CompletableFuture<Any?>
}
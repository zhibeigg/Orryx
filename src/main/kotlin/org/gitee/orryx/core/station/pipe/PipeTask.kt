package org.gitee.orryx.core.station.pipe

import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * 管道任务实现类
 *
 * 管道任务是一个可以被中断或超时完成的异步任务，支持以下特性：
 * - 通过指定的触发器(Trigger)中断任务
 * - 超时自动完成
 * - 可选的周期性子任务
 * - 线程安全的状态管理
 *
 * @param uuid 任务的唯一标识符
 * @param scriptContext Kether脚本上下文，用于脚本执行时的变量传递
 * @param brokeTriggers 能够中断此任务的触发器名称集合
 * @param timeout 任务超时时间(tick)，超时后自动调用[complete]
 * @param onBrock 任务被中断时的回调函数
 * @param onComplete 任务正常完成时的回调函数
 * @param periodTask 可选的周期任务，会随主任务一起启动和取消
 *
 * @see IPipeTask
 * @see PipeBuilder
 * @see PipeTaskManager
 */
class PipeTask(
    override val uuid: UUID,
    override val scriptContext: ScriptContext?,
    override val brokeTriggers: Set<String>,
    override val timeout: Long,
    override val onBrock: PipeTaskCallback,
    override val onComplete: PipeTaskCallback,
    override val periodTask: IPipePeriodTask?
) : IPipeTask {

    companion object {

        /**
         * 创建一个新的管道任务构建器
         *
         * @return 管道任务构建器实例
         */
        fun builder(): PipeBuilder = PipeBuilder()
    }

    /**
     * 任务开始时的时间戳(毫秒)
     */
    override val startStamp: Long = System.currentTimeMillis()

    /**
     * 任务结果的Future，任务关闭时完成
     */
    override val result: CompletableFuture<Any?> = CompletableFuture()

    /**
     * 超时完成的定时任务句柄
     */
    private var completedTask: PlatformExecutor.PlatformTask

    /**
     * 任务是否已关闭的原子标志，确保任务只能被关闭一次
     */
    private val closed = AtomicBoolean(false)

    /**
     * 任务操作锁，保证初始化和关闭操作的线程安全
     */
    private val taskLock = ReentrantLock()

    init {
        taskLock.lock()
        try {
            // 注册超时自动完成的定时任务
            completedTask = submit(delay = timeout) {
                complete()
            }
            // 启动周期任务(如果存在)
            periodTask?.start(this)
            // 将任务注册到管理器
            PipeTaskManager.addPipeTask(this)
        } finally {
            taskLock.unlock()
        }
    }

    /**
     * 中断任务
     *
     * 当任务被触发器中断时调用，会执行[onBrock]回调
     *
     * @return 回调函数执行的结果Future
     */
    override fun broke(): CompletableFuture<Any?> {
        return close(onBrock)
    }

    /**
     * 完成任务
     *
     * 当任务超时或手动完成时调用，会执行[onComplete]回调
     *
     * @return 回调函数执行的结果Future
     */
    override fun complete(): CompletableFuture<Any?> {
        return close(onComplete)
    }

    /**
     * 关闭任务的内部方法
     *
     * 使用CAS操作确保任务只能被关闭一次，关闭时会：
     * 1. 取消超时定时任务
     * 2. 取消周期任务(如果存在)
     * 3. 从管理器中移除此任务
     * 4. 执行指定的回调函数
     * 5. 完成结果Future
     *
     * @param func 关闭时要执行的回调函数
     * @return 回调函数执行的结果Future
     */
    internal fun close(func: PipeTaskCallback): CompletableFuture<Any?> {
        if (!closed.compareAndSet(false, true)) return result

        taskLock.lock()
        try {
            completedTask.cancel()
            periodTask?.cancel(this)
            PipeTaskManager.removePipeTask(this)
            return func.invoke(this).whenComplete { _, _ ->
                result.complete(null)
            }
        } finally {
            taskLock.unlock()
        }
    }
}
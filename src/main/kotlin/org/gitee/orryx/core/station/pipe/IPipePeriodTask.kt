package org.gitee.orryx.core.station.pipe

/**
 * 管道周期任务接口。
 *
 * @property onPeriod 每周期执行一次的回调
 * @property period 周期长度（Tick）
 */
interface IPipePeriodTask {

    val onPeriod: PipeTaskConsumer

    val period: Long

    /**
     * 启动时执行。
     *
     * @param pipeTask 管式任务
     */
    fun start(pipeTask: IPipeTask)

    /**
     * 结束时执行。
     *
     * @param pipeTask 管式任务
     */
    fun cancel(pipeTask: IPipeTask)
}

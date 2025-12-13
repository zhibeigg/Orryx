package org.gitee.orryx.core.station.pipe

interface IPipePeriodTask {

    /**
     * 每周期执行一次
     * */
    val onPeriod: PipeTaskConsumer

    /**
     * 多少Tick为一周期
     * */
    val period: Long

    /**
     * 启动时执行
     * @param pipeTask 管式任务
     * */
    fun start(pipeTask: IPipeTask)

    /**
     * 结束时执行
     * @param pipeTask 管式任务
     * */
    fun cancel(pipeTask: IPipeTask)
}
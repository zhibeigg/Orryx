package org.gitee.orryx.module.state

interface IRunningState {

    /**
     * 状态配置文件
     * */
    val state: IActionState

    /**
     * 是否已结束
     * */
    val stop: Boolean

    /**
     * 开始此状态
     * */
    fun start()

    /**
     * 强制停止此状态
     * */
    fun stop()

    /**
     * 是否能过渡到下一个状态
     * @param nextRunningState 下一个状态
     * @return 是否能
     * */
    fun hasNext(nextRunningState: IRunningState): Boolean
}
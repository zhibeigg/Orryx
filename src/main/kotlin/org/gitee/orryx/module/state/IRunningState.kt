package org.gitee.orryx.module.state

/**
 * 运行中的状态接口。
 *
 * @property state 状态配置
 * @property stop 是否已结束
 */
interface IRunningState {

    val state: IActionState

    val stop: Boolean

    /**
     * 开始此状态。
     */
    fun start()

    /**
     * 强制停止此状态。
     */
    fun stop()

    /**
     * 是否能过渡到下一个状态。
     *
     * @param nextRunningState 下一个运行状态
     * @return 是否允许过渡
     */
    fun hasNext(nextRunningState: IRunningState): Boolean
}

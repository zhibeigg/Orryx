package org.gitee.orryx.api.interfaces

import org.gitee.orryx.core.common.timer.ITimer

/**
 * 计时器 API 接口。
 *
 * 提供对技能计时器和中转站计时器的访问。
 *
 * @property skillTimer 技能计时器，用于管理技能冷却等计时任务
 * @property stationTimer 中转站计时器，用于管理中转站相关的计时任务
 */
interface ITimerAPI {

    val skillTimer: ITimer

    val stationTimer: ITimer
}

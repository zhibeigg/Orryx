package org.gitee.orryx.api.interfaces

import org.gitee.orryx.core.common.timer.ITimer

interface ITimerAPI {

    /**
     * 获取技能计时器
     * @return 计时器
     * */
    val skillTimer: ITimer

    /**
     * 获取中转站计时器
     * @return 计时器
     * */
    val stationTimer: ITimer

}
package org.gitee.orryx.api

import org.gitee.orryx.api.interfaces.ITimerAPI
import org.gitee.orryx.core.common.timer.ITimer
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.common.timer.StationTimer
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory

class TimerAPI: ITimerAPI {

    override val skillTimer: ITimer
        get() = SkillTimer

    override val stationTimer: ITimer
        get() = StationTimer


    companion object {

        @Awake(LifeCycle.CONST)
        fun init() {
            PlatformFactory.registerAPI<ITimerAPI>(TimerAPI())
        }

    }

}
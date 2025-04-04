package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.states.DodgeState.Running
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.Script

class SkillState(override val key: String, val skill: ICastSkill): IActionState {

    override val script: Script? = null

    class Running(val data: PlayerData, override val state: SkillState, val duration: Long): IRunningState {

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            task = submit(delay = duration) {
                stop = true
                StateManager.callNext(data.player)
            }
        }

        override fun stop() {
            task?.cancel()
            stop = true
        }

        override fun hasNext(runningState: IRunningState): Boolean {
            if (stop) return true
            return when (runningState) {
                is DodgeState.Running -> false
                is BlockState.Running -> false
                is GeneralAttackState.Running -> false
                is Running -> false
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }

    }

}
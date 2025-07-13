package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.module.state.AbstractRunningState
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.Script

class SkillState(val skill: ICastSkill): IActionState {

    override val key: String = skill.key

    override val script: Script? = null

    class Running(override val data: PlayerData, override val state: SkillState, val duration: Long): AbstractRunningState(data) {

        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            task = submit(delay = duration + 1) {
                stop = true
                StateManager.callNext(data.player)
            }
        }

        override fun stop() {
            task?.cancel()
            stop = true
        }

        override fun hasNext(nextRunningState: IRunningState): Boolean {
            if (!super.hasNext(nextRunningState)) return false
            if (stop) return true
            return when (nextRunningState) {
                is DodgeState.Running -> false
                is BlockState.Running -> false
                is GeneralAttackState.Running -> false
                is PressGeneralAttackState.Running -> false
                is Running -> false
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
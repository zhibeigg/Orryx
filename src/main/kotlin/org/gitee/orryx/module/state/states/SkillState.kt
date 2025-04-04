package org.gitee.orryx.module.state.states

import org.bukkit.entity.Player
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.StateManager
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.kether.Script

class SkillState(override val key: String, val duration: Long): IActionState {

    override fun hasNext(input: String): Boolean {
        return false
    }

    override val script: Script? = null

    class Running(val player: Player, override val state: SkillState): IRunningState {

        var startTimestamp: Long = 0
            private set

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            startTimestamp = System.currentTimeMillis()
            task = submit(delay = state.duration) {
                stop = true
                StateManager.callNext(player)
            }
        }

        override fun stop() {
            task?.cancel()
            stop = true
            StateManager.callNext(player)
        }

    }

}
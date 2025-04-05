package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.states.DodgeState.Running
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import kotlin.math.max

class GeneralAttackState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val connection = configurationSection.getString("Connection").toLongPair("-")

    class Animation(configurationSection: ConfigurationSection) {
        val startKey = configurationSection.getString("Key")!!
        val duration = configurationSection.getLong("Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(val data: PlayerData, override val state: GeneralAttackState): IRunningState {

        var startTimestamp: Long = 0
            private set

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            startTimestamp = System.currentTimeMillis()
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.startKey, 1f)
            }
            submit(delay = max(state.animation.duration, state.connection.second)) {
                stop = true
                if (data.nowRunningState == this) {
                    data.clearRunningState()
                }
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
                is DodgeState.Running -> true
                is BlockState.Running -> true
                is Running -> (System.currentTimeMillis() - startTimestamp) in state.connection.first * 50 until state.connection.second * 50
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }

    }

}
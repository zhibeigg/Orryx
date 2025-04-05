package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.MoveState.*
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.states.BlockState.Running
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import kotlin.math.max

class DodgeState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val invincible = configurationSection.getString("Invincible").toLongPair("-")

    class Animation(configurationSection: ConfigurationSection) {
        val front = configurationSection.getString("Front")!!
        val rear = configurationSection.getString("Rear")!!
        val left = configurationSection.getString("Left")!!
        val right = configurationSection.getString("Right")!!
        val duration = configurationSection.getLong("Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(val data: PlayerData, override val state: DodgeState): IRunningState {

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            val key = when(data.moveState) {
                FRONT -> state.animation.front
                REAR -> state.animation.rear
                LEFT -> state.animation.left
                RIGHT -> state.animation.right
            }
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, key, 1f)
            }
            task = submit(delay = state.invincible.first) {
                if (!Orryx.api().profileAPI.isInvincible(data.player)) {
                    Orryx.api().profileAPI.setInvincible(data.player, state.invincible.second - state.invincible.first)
                }
                task = submit(delay = max(state.invincible.second - state.invincible.first, state.animation.duration - state.invincible.first)) {
                    stop = true
                    StateManager.callNext(data.player)
                }
            }
        }

        override fun stop() {
            task?.cancel()
            stop = true
        }

        override fun hasNext(runningState: IRunningState): Boolean {
            if (stop) return true
            return when (runningState) {
                is Running -> false
                is BlockState.Running -> false
                is GeneralAttackState.Running -> false
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }

    }

}
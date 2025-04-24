package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
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
import taboolib.module.kether.ScriptContext

class DodgeState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val invincible = configurationSection.getString("Invincible").toLongPair("-")
    val connection = configurationSection.getString("Connection").toLongPair("-")

    class Animation(configurationSection: ConfigurationSection) {
        val front = configurationSection.getString("Front")!!
        val rear = configurationSection.getString("Rear")!!
        val left = configurationSection.getString("Left")!!
        val right = configurationSection.getString("Right")!!
        val duration = configurationSection.getLong("Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(val data: PlayerData, override val state: DodgeState): IRunningState {

        var startTimestamp: Long = 0
            private set

        override var stop: Boolean = false
            private set

        private var task0: PlatformExecutor.PlatformTask? = null
        private var task1: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null

        override fun start() {
            startTimestamp = System.currentTimeMillis()
            state.runScript(data) { context = this }
            val key = when(data.moveState) {
                FRONT -> state.animation.front
                REAR -> state.animation.rear
                LEFT -> state.animation.left
                RIGHT -> state.animation.right
            }
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, key, 1f)
            }
            if(state.connection.first != 0L) {
                task1 = submit(delay = state.connection.first) {
                    StateManager.callNext(data.player)
                }
            }
            task0 = submit(delay = state.invincible.first) {
                if (!Orryx.api().profileAPI.isInvincible(data.player)) {
                    Orryx.api().profileAPI.setInvincible(data.player, (state.invincible.second - state.invincible.first) * 50)
                }
                task0 = submit(delay = state.animation.duration - state.invincible.first + 1) {
                    stop = true
                    StateManager.callNext(data.player)
                    val lessTime = state.connection.second - state.animation.duration - 1
                    if (lessTime > 0) {
                        submit(delay = lessTime) {
                            if (data.nowRunningState == this@Running) {
                                data.clearRunningState()
                            }
                        }
                    }
                }
            }
        }

        override fun stop() {
            task0?.cancel()
            task1?.cancel()
            context?.apply {
                terminate()
                ScriptManager.cleanUp(id)
            }
            stop = true
        }

        override fun hasNext(runningState: IRunningState): Boolean {
            if (stop) return true
            return when (runningState) {
                is Running -> (System.currentTimeMillis() - startTimestamp) in state.connection.first * 50 until state.connection.second * 50
                is BlockState.Running -> false
                is GeneralAttackState.Running -> false
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
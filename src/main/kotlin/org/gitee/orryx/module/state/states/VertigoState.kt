package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.module.state.AbstractRunningState
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.getNearPlayers
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext

class VertigoState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    class Animation(configurationSection: ConfigurationSection) {
        val startKey: K = configurationSection.getString("Start-Key")!!
        val loopKey: K = configurationSection.getString("Loop-Key")!!
        val endKey: K = configurationSection.getString("End-Key")!!
        val startDuration: getLong = configurationSection.getLong("Start-Duration")
        val loopDuration: getLong = configurationSection.getLong("Loop-Duration")
        val endDuration: getLong = configurationSection.getLong("End-Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(override val data: PlayerData, override val state: VertigoState): AbstractRunningState(data) {

        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null

        override fun start() {
            state.runScript(data) { context = this }
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.startKey, 1f)
            }
            task = submit(delay = state.animation.startDuration) {
                getNearPlayers(data.player) { viewer ->
                    IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.loopKey, 1f)
                }
                task = submit(delay = state.animation.loopDuration) {
                    getNearPlayers(data.player) { viewer ->
                        IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.endKey, 1f)
                    }
                    task = submit(delay = state.animation.endDuration + 1) {
                        stop = true
                        StateManager.callNext(data.player)
                    }
                }
            }
        }

        override fun stop() {
            task?.cancel()
            context?.apply {
                terminate()
                ScriptManager.cleanUp(id)
            }
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
                is SkillState.Running -> false
                is Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
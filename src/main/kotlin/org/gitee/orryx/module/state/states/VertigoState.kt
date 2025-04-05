package org.gitee.orryx.module.state.states

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.getNearPlayers
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script

class VertigoState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    class Animation(configurationSection: ConfigurationSection) {
        val startKey = configurationSection.getString("Start-Key")!!
        val loopKey = configurationSection.getString("Loop-Key")!!
        val endKey = configurationSection.getString("End-Key")!!
        val startDuration = configurationSection.getLong("Start-Duration")
        val loopDuration = configurationSection.getLong("Loop-Duration")
        val endDuration = configurationSection.getLong("End-Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(val player: Player, override val state: VertigoState): IRunningState {

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            getNearPlayers(player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, player, state.animation.startKey, 1f)
            }
            task = submit(delay = state.animation.startDuration) {
                getNearPlayers(player) { viewer ->
                    IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, player, state.animation.loopKey, 1f)
                }
                task = submit(delay = state.animation.loopDuration) {
                    getNearPlayers(player) { viewer ->
                        IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, player, state.animation.endKey, 1f)
                    }
                    task = submit(delay = state.animation.endDuration) {
                        stop = true
                        StateManager.callNext(player)
                    }
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
                is DodgeState.Running -> false
                is BlockState.Running -> false
                is GeneralAttackState.Running -> false
                is SkillState.Running -> false
                is Running -> !Orryx.api().profileAPI.isSuperBody(player)
                else -> false
            }
        }

    }

}
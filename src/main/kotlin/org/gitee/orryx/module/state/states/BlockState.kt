package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script

class BlockState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val check = configurationSection.getString("Check").toLongPair("-")
    val invincible = configurationSection.getLong("Invincible")

    class Animation(configurationSection: ConfigurationSection) {
        val key = configurationSection.getString("Key")!!
        val duration = configurationSection.getLong("Duration")
        val success = configurationSection.getString("SuccessKey")!!
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(val data: PlayerData, override val state: BlockState): IRunningState {

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        fun success() {
            task?.cancel()
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.success, 1f)
            }
            Orryx.api().profileAPI.setInvincible(data.player, state.invincible)
            Orryx.api().profileAPI.cancelBlock(data.player)
            stop = true
        }

        override fun start() {
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.key, 1f)
            }
            task = submit(delay = state.animation.duration) {
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
                is Running -> false
                is DodgeState.Running -> true
                is GeneralAttackState.Running -> false
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }

    }

}
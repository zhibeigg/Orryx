package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
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
import taboolib.module.kether.ScriptContext
import kotlin.math.max

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

        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null

        fun success() {
            task?.cancel()
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.success, 1f)
            }
            Orryx.api().profileAPI.setInvincible(data.player, state.invincible * 50)
            Orryx.api().profileAPI.cancelBlock(data.player)
            stop = true
        }

        override fun start() {
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.key, 1f)
            }
            state.runScript(data) { context = this }
            task = submit(delay = state.check.first) {
                Orryx.api().profileAPI.setBlock(data.player, (state.check.second - state.check.first) * 50)
                task = submit(delay = max(state.animation.duration - state.check.first + 1, state.check.second - state.check.first + 1)) {
                    stop = true
                    StateManager.callNext(data.player)
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
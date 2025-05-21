package org.gitee.orryx.module.state.states

import eos.moe.dragoncore.network.PacketSender
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.module.state.AbstractRunningState
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.PlayerData
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import kotlin.math.max

class GeneralAttackState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val connection = configurationSection.getString("Connection").toLongPair("-")

    class Animation(configurationSection: ConfigurationSection) {
        val startKey = configurationSection.getString("Key")!!
        val duration = configurationSection.getLong("Duration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(override val data: PlayerData, override val state: GeneralAttackState): AbstractRunningState(data) {

        var startTimestamp: Long = 0
            private set

        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null

        override fun start() {
            state.runScript(data) { context = this }
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.startKey, 1f)
            }
            task = submit(delay = state.connection.first + 1) {
                sendPacket()
                startTimestamp = System.currentTimeMillis()
                StateManager.callNext(data.player)
                task = submit(delay = max(state.animation.duration, state.connection.second) - state.connection.first) {
                    stop = true
                    if (data.nowRunningState == this@Running) {
                        data.clearRunningState()
                    }
                }
            }
        }

        fun sendPacket() {
            if (!DragonCorePlugin.isEnabled) return
            PacketSender.sendSyncPlaceholder(data.player, mapOf(
                "order_time" to "${state.connection.second - state.connection.first}"
            ))
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
                is DodgeState.Running -> true
                is BlockState.Running -> true
                is Running -> (System.currentTimeMillis() - startTimestamp) in 0..state.connection.second * 50L
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
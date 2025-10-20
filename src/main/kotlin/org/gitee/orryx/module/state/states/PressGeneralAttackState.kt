package org.gitee.orryx.module.state.states

import eos.moe.dragoncore.network.PacketSender
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.module.state.*
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

class PressGeneralAttackState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val connection = configurationSection.getString("Connection").toLongPair("-")

    class Animation(configurationSection: ConfigurationSection) {
        val startKey = configurationSection.getString("StartKey")!!
        val castKey = configurationSection.getString("CastKey")!!
        val pressDuration = configurationSection.getLong("PressDuration")
        val castDuration = configurationSection.getLong("CastDuration")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    class Running(override val data: PlayerData, override val state: PressGeneralAttackState): AbstractRunningState(data) {

        val attackSpeed: Float = data.getAttackSpeed()

        var startTimestamp: Long = 0
            private set

        var pressStartTimestamp: Long = 0
            private set

        var cast: Boolean = false
        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null

        override fun start() {
            val pressDuration = ceil(state.animation.pressDuration / attackSpeed).toLong()
            pressStartTimestamp = System.currentTimeMillis()
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.startKey, attackSpeed)
            }
            task = submit(delay = pressDuration) {
                castAttack()
            }
        }

        fun castAttack() {
            if (!cast) {
                val castDuration = ceil(state.animation.castDuration / attackSpeed).toLong()
                val connectionDuration1 = ceil(state.connection.first / attackSpeed).toLong()
                val connectionDuration2 = ceil(state.connection.second / attackSpeed).toLong()
                cast = true
                state.runScript(data) {
                    val tick = (((System.currentTimeMillis() - pressStartTimestamp)*attackSpeed)/50L).roundToLong()
                    set("pressTick", tick)
                    set("attackSpeed", attackSpeed)
                    context = this
                }
                getNearPlayers(data.player) { viewer ->
                    IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.castKey, attackSpeed)
                }
                task = submit(delay = connectionDuration1 + 1) {
                    sendPacket()
                    startTimestamp = System.currentTimeMillis()
                    StateManager.callNext(data.player)
                    task = submit(delay = max(castDuration, connectionDuration2) - connectionDuration1) {
                        stop = true
                        if (data.nowRunningState == this@Running) {
                            data.clearRunningState()
                        }
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
                is GeneralAttackState.Running -> cast && (System.currentTimeMillis() - startTimestamp) in 0..state.connection.second * 50L
                is Running -> cast && (System.currentTimeMillis() - startTimestamp) in 0..state.connection.second * 50L
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
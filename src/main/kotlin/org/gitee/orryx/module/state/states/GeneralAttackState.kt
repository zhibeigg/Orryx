package org.gitee.orryx.module.state.states

import org.bukkit.entity.Player
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.IRunningState
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import kotlin.math.max

class GeneralAttackState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    class Animation(configurationSection: ConfigurationSection) {
        val startKey = configurationSection.getString("Key")!!
        val duration = configurationSection.getLong("Duration")
        val connection = configurationSection.getString("Connection").toLongPair("-")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    override fun hasNext(input: String): Boolean {
        TODO("Not yet implemented")
    }

    class Running(val player: Player, override val state: GeneralAttackState): IRunningState {

        var startTimestamp: Long = 0
            private set

        var stop: Boolean = false
            private set

        var task: PlatformExecutor.PlatformTask? = null

        override fun start() {
            startTimestamp = System.currentTimeMillis()
            getNearPlayers(player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, player, state.animation.startKey, 1f)
            }
            submit(delay = max(state.animation.duration, state.animation.connection.second)) {
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
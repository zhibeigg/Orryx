package org.gitee.orryx.module.state.states

import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.compat.IAnimationBridge
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StateParameter
import org.gitee.orryx.module.spirit.ISpiritManager
import org.gitee.orryx.module.state.*
import org.gitee.orryx.utils.getNearPlayers
import org.gitee.orryx.utils.toLongPair
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import kotlin.math.max

class BlockState(override val key: String, override val configurationSection: ConfigurationSection): IActionState, ISpiritCost {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val check = configurationSection.getString("Check").toLongPair("-")
    val invincible = configurationSection.getLong("Invincible")
    val blockType = configurationSection.getEnum("DamageType", DamageType::class.java) ?: DamageType.PHYSICS
    override val spirit = configurationSection.getDouble("Spirit", 0.0)

    class Animation(configurationSection: ConfigurationSection) {
        val key = configurationSection.getString("Key")!!
        val duration = configurationSection.getLong("Duration")
        val success = configurationSection.getString("SuccessKey")!!
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }
    val blockScript: Script? = configurationSection.getString("BlockAction")?.let { StateManager.loadScript(this, it) }

    class Running(override val data: PlayerData, override val state: BlockState): AbstractRunningState(data) {

        override var stop: Boolean = false
            private set

        private var task: PlatformExecutor.PlatformTask? = null
        private var context: ScriptContext? = null
        private var successContext: ScriptContext? = null

        fun success(event: OrryxDamageEvents.Pre) {
            task?.cancel()
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.success, 1f)
            }
            Orryx.api().profileAPI.setInvincible(data.player, state.invincible * 50)
            Orryx.api().profileAPI.cancelBlock(data.player)
            stop = true
            state.blockScript?.let {
                ScriptManager.runScript(adaptPlayer(data.player), StateParameter(data), it) {
                    this["event"] = event
                    successContext = this
                }
            }
        }

        override fun start() {
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.key, 1f)
            }
            ISpiritManager.INSTANCE.takeSpirit(data.player, state.spirit)
            state.runScript(data) { context = this }
            task = submit(delay = state.check.first) {
                Orryx.api().profileAPI.setBlock(data.player, state.blockType, (state.check.second - state.check.first) * 50) {
                    success(it)
                }
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
            successContext?.apply {
                terminate()
                ScriptManager.cleanUp(id)
            }
            stop = true
        }

        override fun hasNext(nextRunningState: IRunningState): Boolean {
            if (!super.hasNext(nextRunningState)) return false
            if (stop) return true
            return when (nextRunningState) {
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
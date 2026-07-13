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

class BlockState(override val key: String, configurationSection: ConfigurationSection): IActionState, ISpiritCost {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    val check = configurationSection.getString("Check").toLongPair("-")
    val invincible = configurationSection.getLong("Invincible")

    /**
     * 可格挡的伤害类型列表。
     *
     * 支持两种写法：
     * - 单个：`DamageType: PHYSICS`
     * - 列表：`DamageType: [PHYSICS, MAGIC, MONSTER]`
     *
     * 未配置或解析全部失败时默认 [DamageType.PHYSICS]。
     */
    val blockTypes: List<DamageType> = parseBlockTypes(configurationSection)

    override val spirit = configurationSection.getDouble("Spirit", 0.0)

    private fun parseBlockTypes(configurationSection: ConfigurationSection): List<DamageType> {
        val raw = configurationSection.get("DamageType") ?: return listOf(DamageType.PHYSICS)
        val names = when (raw) {
            is Collection<*> -> raw.mapNotNull { it?.toString() }
            else -> listOf(raw.toString())
        }
        val types = names.mapNotNull { name ->
            runCatching { DamageType.valueOf(name.trim().uppercase()) }.getOrNull()
        }.distinct()
        return types.ifEmpty { listOf(DamageType.PHYSICS) }
    }

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
            StateManager.callNext(data.player)
        }

        override fun start() {
            getNearPlayers(data.player) { viewer ->
                IAnimationBridge.INSTANCE.setPlayerAnimation(viewer, data.player, state.animation.key, 1f)
            }
            state.runScript(data) { context = this }
            task = submit(delay = state.check.first) {
                state.blockTypes.forEach { blockType ->
                    Orryx.api().profileAPI.setBlock(data.player, blockType, (state.check.second - state.check.first) * 50) {
                        success(it)
                    }
                }
                task = submit(delay = max(state.animation.duration - state.check.first + 1, state.check.second - state.check.first + 1)) {
                    stop = true
                    if (data.nowRunningState == this@Running) {
                        data.clearRunningState()
                    }
                    StateManager.callNext(data.player)
                }
            }
        }

        override fun stop() {
            task?.cancel()
            context?.apply {
                terminate()
                ScriptManager.cleanUp(this)
            }
            successContext?.apply {
                terminate()
                ScriptManager.cleanUp(this)
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
                is PressGeneralAttackState.Running -> false
                is SkillState.Running -> true
                is VertigoState.Running -> !Orryx.api().profileAPI.isSuperBody(data.player)
                else -> false
            }
        }
    }
}
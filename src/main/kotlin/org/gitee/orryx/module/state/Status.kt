package org.gitee.orryx.module.state

import com.eatthepath.uuid.FastUUID
import kotlinx.coroutines.future.future
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kether.ScriptManager.runKether
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.parse
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.warning
import taboolib.common5.cfloat
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.orNull
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class Status(override val key: String, configuration: Configuration): IStatus {

    val options = Options(key, configuration.getConfigurationSection("Options")!!)
    val privateStates: mutableMapOf = mutableMapOf<String, IActionState>()

    init {
        configuration.getConfigurationSection("States")?.getKeys(false)?.forEach {
            privateStates[it] = StateManager.load(it, configuration.getConfigurationSection("States.$it")!!)
        }
    }

    class Options(val key: String, configurationSection: ConfigurationSection) {
        val conditionAction: K = configurationSection.getString("Condition")!!
        val cancelHeldEventWhenPlaying: getBoolean = configurationSection.getBoolean("CancelHeldEventWhenPlaying", true)
        val cancelBukkitAttack: getBoolean = configurationSection.getBoolean("CancelBukkitAttack", false)
        val attackSpeedAction: K = configurationSection.getString("AttackSpeed", "1.0")!!

        // 龙核附属
        val controller = if (DragonCorePlugin.isEnabled) {
            configurationSection.getString("Controller") ?: error("请配置龙核控制器 Status: $key")
        } else {
            null
        }

        // 萌芽附属
        val animationState = if (GermPluginPlugin.isEnabled) {
            configurationSection.getString("AnimationState") ?: error("请配置萌芽动作状态 Status: $key")
        } else {
            null
        }

        // 时装
        private val armourers = configurationSection.getStringList("Armourers")

        fun getArmourers(player: Player): List<String> {
            return player.parse(armourers, emptyMap())
        }

        fun getAttackSpeed(player: Player): Float {
            return player.eval(attackSpeedAction, emptyMap()).orNull()?.cfloat ?: 1.0f
        }

        fun getCondition(player: Player): CompletableFuture<Any?> {
            return OrryxAPI.pluginScope.future {
                player.eval(conditionAction, emptyMap()).get(100, TimeUnit.MILLISECONDS)
            }
        }
    }

    val script: Script? = StateManager.loadScript(this, configuration.getString("Action")!!)

    override fun next(playerData: PlayerData, input: String): CompletableFuture<IRunningState?> {
        val script = script ?: run {
            warning("请检查Action status: ${playerData.status?.key} 输入: $input")
            return CompletableFuture.completedFuture(null)
        }
        return runKether(CompletableFuture.completedFuture(null)) {
            ScriptContext.create(script).also {
                it.sender = adaptPlayer(playerData.player)
                it.id = FastUUID.toString(UUID.randomUUID())
                it["input"] = input
            }.runActions().thenApply {
                it as IRunningState?
            }
        }
    }
}
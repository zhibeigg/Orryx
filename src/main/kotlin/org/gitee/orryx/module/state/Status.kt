package org.gitee.orryx.module.state

import org.bukkit.entity.Player
import org.gitee.orryx.module.state.states.SkillState
import org.gitee.orryx.utils.checkAndCast
import org.gitee.orryx.utils.getActionType
import org.gitee.orryx.utils.getKeySort
import org.gitee.orryx.utils.getTimeout
import org.gitee.orryx.utils.keySetting
import org.gitee.orryx.utils.parse
import org.gitee.orryx.utils.tryCast
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

class Status(override val key: String, configuration: Configuration): IStatus {

    val options = Options(configuration.getConfigurationSection("Options")!!)
    val privateStates = mutableMapOf<String, IActionState>()

    init {
        configuration.getConfigurationSection("States")?.getKeys(false)?.forEach {
            privateStates[it] = StateManager.load(it, configuration.getConfigurationSection("States.$it")!!)
        }
    }

    class Options(configurationSection: ConfigurationSection) {
        val conditionAction = configurationSection.getString("Condition")!!
        val cancelHeldEventWhenPlaying = configurationSection.getBoolean("CancelHeldEventWhenPlaying", true)
        val cancelBukkitAttack = configurationSection.getBoolean("CancelBukkitAttack", false)
        val controller = configurationSection.getString("Controller")!!
        private val armourers = configurationSection.getStringList("Armourers")

        fun getArmourers(player: Player): List<String> {
            return player.parse(armourers, emptyMap())
        }
    }

    val script: Script? = StateManager.loadScript(this, configuration.getString("Action")!!)

    override fun next(playerData: PlayerData, input: String): CompletableFuture<IRunningState?> {
        val script = script ?: run {
            warning("请检查Action status: ${playerData.status?.key} 输入: $input")
            return CompletableFuture.completedFuture(null)
        }
        return ScriptContext.create(script).also {
            it.sender = adaptPlayer(playerData.player)
            it.id = UUID.randomUUID().toString()
            it["input"] = input
        }.runActions().thenApply {
            it as IRunningState?
        }
    }
}
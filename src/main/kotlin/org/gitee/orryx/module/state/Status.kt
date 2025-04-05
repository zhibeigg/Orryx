package org.gitee.orryx.module.state

import taboolib.common.platform.function.adaptPlayer
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

class Status(override val key: String, configuration: Configuration): IStatus {

    val options = Options(configuration.getConfigurationSection("options")!!)
    val privateStates = mutableMapOf<String, IActionState>()

    init {
        configuration.getConfigurationSection("States")?.getKeys(false)?.forEach {
            privateStates[it] = StateManager.load(it, configuration.getConfigurationSection("States.$it")!!)
        }
    }

    class Options(configurationSection: ConfigurationSection) {
        val conditionAction  = configurationSection.getString("Condition")!!
        val cancelHeldEventWhenPlaying = configurationSection.getBoolean("CancelHeldEventWhenPlaying", true)
        val controller = configurationSection.getString("Controller")!!
        val armourers = configurationSection.getStringList("Armourers")
    }

    val script: Script = StateManager.loadScript(this, configuration.getString("Action")!!)

    override fun next(playerData: PlayerData, input: String): CompletableFuture<IRunningState?> {
        return ScriptContext.create(script).also {
            it.sender = adaptPlayer(playerData.player)
            it.id = UUID.randomUUID().toString()
            it["input"] = input
        }.runActions().thenApply {
            it as IRunningState?
        }
    }

}
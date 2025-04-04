package org.gitee.orryx.module.state.states

import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.toIntPair
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script

class DodgeState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    class Animation(configurationSection: ConfigurationSection) {
        val front = configurationSection.getString("Front")
        val rear = configurationSection.getString("Rear")
        val left = configurationSection.getString("Left")
        val right = configurationSection.getString("Right")
        val duration = configurationSection.getInt("Duration")
        val invincible = configurationSection.getString("invincible").toIntPair("-")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    override fun hasNext(input: String): Boolean {
        TODO("Not yet implemented")
    }

}
package org.gitee.orryx.module.state.states

import org.gitee.orryx.module.state.IActionState
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.toIntPair
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.kether.Script

class BlockState(override val key: String, configurationSection: ConfigurationSection): IActionState {

    val animation: Animation = Animation(configurationSection.getConfigurationSection("Animation")!!)

    class Animation(configurationSection: ConfigurationSection) {
        val key = configurationSection.getString("Key")
        val duration = configurationSection.getInt("Duration")
        val check = configurationSection.getString("Check").toIntPair("-")
        val end = configurationSection.getInt("End")
    }

    override val script: Script? = configurationSection.getString("Action")?.let { StateManager.loadScript(this, it) }

    override fun hasNext(input: String): Boolean {
        TODO("Not yet implemented")
    }

}
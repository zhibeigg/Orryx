package org.gitee.orryx.core.selector.presets

import org.gitee.orryx.core.selector.ISelectorPresets
import taboolib.library.configuration.ConfigurationSection

class PresetsLoader(override val keys: Array<String>, val configurationSection: ConfigurationSection): ISelectorPresets {

    override val action: String
        get() = configurationSection.getString("Actions") ?: error("selector预设${configurationSection.name}未书写Actions键")

}
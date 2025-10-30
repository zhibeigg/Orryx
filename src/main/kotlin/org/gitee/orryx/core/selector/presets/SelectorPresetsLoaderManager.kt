package org.gitee.orryx.core.selector.presets

import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

object SelectorPresetsLoaderManager {

    @Config("selectors.yml")
    lateinit var selectors: ConfigFile
        private set

    private val selectorPresets by unsafeLazy { hashMapOf<String, PresetsLoader>() }

    internal fun getSelectorPreset(key: String): PresetsLoader? {
        return selectorPresets[key]
    }

    @Reload(weight = 1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        selectorPresets.clear()
        selectors.reload()
        selectors.getKeys(false).forEach { key ->
            selectorPresets[key] = PresetsLoader(arrayOf(key), selectors.getConfigurationSection(key)!!)
        }
        consoleMessage("&e┣&7Selectors loaded &e${selectorPresets.size} &a√")
    }
}
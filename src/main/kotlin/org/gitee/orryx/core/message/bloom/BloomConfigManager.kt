package org.gitee.orryx.core.message.bloom

import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.consoleMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import java.util.concurrent.ConcurrentHashMap

object BloomConfigManager {

    @Config("bloom.yml")
    lateinit var config: ConfigFile
        private set

    private var syncDelay = 40L
    private val configs = ConcurrentHashMap<String, BloomConfig>()

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun load() {
        config.reload()
        syncDelay = config.getLong("sync-delay", 40L)
        configs.clear()
        config.getConfigurationSection("configs")?.getKeys(false)?.forEach { id ->
            val section = config.getConfigurationSection("configs.$id") ?: return@forEach
            val color = section.getIntegerList("color")
            if (color.size < 4) return@forEach
            configs[id] = BloomConfig(
                id = id,
                name = section.getString("name", "")!!,
                r = color[0],
                g = color[1],
                b = color[2],
                a = color[3],
                strength = section.getDouble("strength", 1.0).toFloat(),
                radius = section.getDouble("radius", 30.0).toFloat(),
                priority = section.getInt("priority", 0)
            )
        }
        consoleMessage("&e┣&7Bloom configs loaded &e${configs.size} &a√")
    }

    fun getConfigs(): Map<String, BloomConfig> = configs

    fun getSyncDelay(): Long = syncDelay
}

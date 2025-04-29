package org.gitee.orryx.core.station.stations

import org.gitee.orryx.utils.getMap
import taboolib.common.platform.event.EventPriority
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class StationLoader(override val key: String, val configuration: Configuration): IStation {

    val options by unsafeLazy { configuration.getConfigurationSection("Options") ?: error("中转站${key}位于${configuration.file}未书写Options") }

    override val event: String = options.getString("Event")?.uppercase() ?: error("中转站${key}位于${configuration.file}未书写Event")

    override val baffleAction: String? = options.getString("BaffleAction")

    override val weight: Int = options.getInt("Weight", 0)

    override val priority: EventPriority = EventPriority.valueOf(options.getString("Priority", "NORMAL")!!.uppercase())

    override val ignoreCancelled: Boolean = options.getBoolean("IgnoreCancelled", false)

    override val variables = options.getMap("Variables").mapKeys { it.key.uppercase() }

    override val actions: String = configuration.getString("Actions") ?: error("中转站${key}位于${configuration.file}未书写Actions")

    override val script: Script? = StationLoaderManager.loadScript(this)
}
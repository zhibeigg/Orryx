package org.gitee.orryx.core.job

import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

class JobLoader(override val key: String, val configuration: Configuration): IJob {

    private val options by unsafeLazy { configuration.getConfigurationSection("Options") ?: error("职业${key}位于${configuration.file}未书写Options键") }

    override val name: String = (options.getString("Name") ?: key).colored()

    override val skills: List<String> = options.getStringList("Skills")

    override val attributes: List<String> = options.getStringList("Attributes")

    override val maxManaActions: String = options.getString("MaxManaActions", "100")!!

    override val regainManaActions: String = options.getString("RegainManaActions", "1")!!

    override val upgradePointActions: String = options.getString("UpgradePointActions", "1")!!

    override val experience: String = options.getString("Experience", "default")!!

    override val maxSpiritActions: K = options.getString("MaxSpiritActions", "100")!!

    override val regainSpiritActions: K = options.getString("RegainSpiritActions", "1")!!
}
package org.gitee.orryx.core.job

import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

class JobLoader(override val key: String, val configuration: Configuration): IJob {

    private val options by lazy { configuration.getConfigurationSection("Options") ?: error("职业${key}位于${configuration.file}未书写Options键") }

    override val name: String
        get() = (options.getString("Name") ?: key).colored()

    override val skills: List<String>
        get() = options.getStringList("Skills")

    override val attributes: List<String>
        get() = options.getStringList("Attributes")

    override val maxManaActions: String
        get() = options.getString("MaxManaActions", "0")!!

    override val regainManaActions: String
        get() = options.getString("RegainManaActions", "0")!!

    override val upgradePointActions: String
        get() = options.getString("UpgradePointActions", "0")!!

    override val experience: String
        get() = options.getString("Experience", "default")!!

}
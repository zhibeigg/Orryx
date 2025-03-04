package org.gitee.orryx.core.experience

import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.platform.ProxyCommandSender
import taboolib.common5.cint
import taboolib.module.configuration.Configuration
import taboolib.module.kether.KetherShell
import taboolib.module.kether.ScriptOptions
import taboolib.module.kether.orNull

class ExperienceLoader(override val key: String, val configuration: Configuration): IExperience {

    val options = configuration.getConfigurationSection("Options") ?: error("经验算法${key}位于${configuration.file}未书写Options键")

    override val minLevel: Int = options.getInt("Min")

    override val maxLevel: Int = options.getInt("Max")

    override val experienceEquation: String = options.getString("ExperienceOfLevel", "0")!!

    init {
        if (minLevel >= maxLevel) error("经验计算器$key 位于${configuration.file} Min必须小于Max")
    }

    override fun getExperienceOfLevel(sender: ProxyCommandSender, level: Int): Int {
        if (level !in minLevel+1..maxLevel) return 0
        return KetherShell.eval(
            experienceEquation,
            ScriptOptions.builder().sender(sender).set("level", level).namespace(orryxEnvironmentNamespaces).build()
        ).orNull().cint
    }

}
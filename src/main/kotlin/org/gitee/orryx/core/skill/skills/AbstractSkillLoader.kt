package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.Description
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.Icon
import org.gitee.orryx.utils.getMap
import taboolib.common.util.unsafeLazy
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

abstract class AbstractSkillLoader(key: String, open val configuration: Configuration): ISkill {

    protected val options by unsafeLazy { configuration.getConfigurationSection("Options") ?: error("技能${key}位于${configuration.file}未书写Options键") }

    override val name: String = (options.getString("Name") ?: key).colored()

    override val sort: Int = options.getInt("Sort", 0)

    override val icon: Icon = Icon(options.getString("Icon", options.getString("Name", key))!!.colored())

    override val xMaterial: String = options.getString("XMaterial") ?: XMaterial.BLAZE_ROD.name

    override val description: Description = Description(options.getStringList("Description"))

    override val isLocked: Boolean = options.getBoolean("IsLocked", false)

    override val minLevel: Int = options.getInt("MinLevel", 1)

    override val maxLevel: Int = options.getInt("MaxLevel", 5)

    override val upgradePointAction: String? = options.getString("UpgradePointAction")

    override val upLevelCheckAction: String? = options.getString("UpLevelCheckAction")

    override val upLevelSuccessAction: String? = options.getString("UpLevelSuccessAction")

    override val variables = options.getMap("Variables").mapKeys { it.key.uppercase() }
}
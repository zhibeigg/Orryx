package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.Description
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.Icon
import org.gitee.orryx.utils.getMap
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration

abstract class AbstractSkillLoader(override val key: String, open val configuration: Configuration): ISkill {

    protected val options by lazy { configuration.getConfigurationSection("Options") ?: error("技能${key}位于${configuration.file}未书写Options键") }

    override val name: String
        get() = (options.getString("Name") ?: key).colored()

    override val icon: Icon
        get() = Icon(options.getString("Icon") ?: name)

    override val xMaterial: String
        get() = options.getString("XMaterial") ?: XMaterial.BLAZE_ROD.name

    override val description: Description
        get() = Description(options.getStringList("Description"))

    override val isLocked: Boolean
        get() = options.getBoolean("IsLocked", false)

    override val minLevel: Int
        get() = options.getInt("MinLevel", 1)

    override val maxLevel: Int
        get() = options.getInt("MaxLevel", 5)

    override val upgradePointAction: String?
        get() = options.getString("UpgradePointAction")

    override val upLevelCheckAction: String?
        get() = options.getString("UpLevelCheckAction")

    override val upLevelSuccessAction: String?
        get() = options.getString("UpLevelSuccessAction")

    override val variables
        get() = options.getMap("Variables").mapKeys { it.key.uppercase() }

}
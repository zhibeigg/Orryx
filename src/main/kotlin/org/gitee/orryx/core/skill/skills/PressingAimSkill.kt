package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.PRESSING_AIM
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class PressingAimSkill(
    override val key: String,
    override val configuration: Configuration
) : AbstractCastSkillLoader(key, configuration), IAim, IPress {

    override val type = PRESSING_AIM

    override val aimMinAction: String
        get() = options.getString("AimMinAction", "5")!!

    override val aimMaxAction: String
        get() = options.getString("AimMaxAction", "5")!!

    override val aimRadiusAction: String
        get() = options.getString("AimRadiusAction", "10")!!

    override val period: Long
        get() = options.getLong("Period")

    override val pressPeriodAction: String
        get() = options.getString("PressPeriodAction", "")!!

    override val maxPressTickAction: String
        get() = options.getString("MaxPressTickAction", "20")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
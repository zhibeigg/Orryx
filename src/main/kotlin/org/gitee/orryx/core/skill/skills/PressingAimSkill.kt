package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.PRESSING_AIM
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class PressingAimSkill(
    key: String,
    configuration: Configuration
) : AbstractCastSkillLoader(key, configuration), IAim, IPress {

    override val type = PRESSING_AIM

    override val aimMinAction: String = options.getString("AimMinAction", "5")!!

    override val aimMaxAction: String = options.getString("AimMaxAction", "5")!!

    override val aimRadiusAction: String = options.getString("AimRadiusAction", "10")!!

    override val period: Long = options.getLong("Period")

    override val pressPeriodAction: String = options.getString("PressPeriodAction", "")!!

    override val maxPressTickAction: String = options.getString("MaxPressTickAction", "20")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
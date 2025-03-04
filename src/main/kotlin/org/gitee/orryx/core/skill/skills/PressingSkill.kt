package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.PRESSING
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class PressingSkill(
    key: String,
    configuration: Configuration
) : AbstractCastSkillLoader(key, configuration), IPress {

    override val type = PRESSING

    override val period: Long = options.getLong("Period")

    override val pressPeriodAction: String = options.getString("PressPeriodAction", "")!!

    override val maxPressTickAction: String = options.getString("MaxPressTickAction", "20")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
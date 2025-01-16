package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.DIRECT_AIM
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class DirectAimSkill(
    override val key: String,
    override val configuration: Configuration
) : AbstractCastSkillLoader(key, configuration) {

    override val type = DIRECT_AIM

    val aimScaleAction: String
        get() = options.getString("AimScaleAction", "5")!!

    val aimRangeAction: String
        get() = options.getString("AimRangeAction", "10")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
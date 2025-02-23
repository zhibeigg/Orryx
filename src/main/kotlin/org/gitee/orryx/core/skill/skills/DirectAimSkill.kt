package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.DIRECT_AIM
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class DirectAimSkill(
    override val key: String,
    override val configuration: Configuration
) : AbstractCastSkillLoader(key, configuration), IAim {

    override val type = DIRECT_AIM

    val aimSizeAction: String
        get() = options.getString("AimSizeAction", "5")!!

    override val aimMinAction: String
        get() = aimSizeAction

    override val aimMaxAction: String
        get() = aimSizeAction

    override val aimRadiusAction: String
        get() = options.getString("AimRadiusAction", "10")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
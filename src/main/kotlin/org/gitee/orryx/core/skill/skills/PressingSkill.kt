package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.PRESSING
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class PressingSkill(
    override val key: String,
    override val configuration: Configuration
) : AbstractCastSkillLoader(key, configuration) {

    override val type = PRESSING

    val pressTickAction: String
        get() = options.getString("PressTickAction", "")!!

    val maxPressTickAction: String
        get() = options.getString("MaxPressTickAction", "20")!!

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
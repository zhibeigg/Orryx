package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.DIRECT
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script

class DirectSkill(
    override val key: String,
    override val configuration: Configuration
) : AbstractCastSkillLoader(key, configuration) {

    override val type = DIRECT

    override val script: Script? = SkillLoaderManager.loadScript(this)

}
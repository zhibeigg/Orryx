package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.utils.PASSIVE
import taboolib.module.configuration.Configuration

class PassiveSkill(
    override val key: String,
    configuration: Configuration
) : AbstractSkillLoader(key, configuration) {

    override val type = PASSIVE

}
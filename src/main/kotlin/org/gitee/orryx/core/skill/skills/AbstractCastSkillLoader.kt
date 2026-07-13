package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.ICastSkill
import org.gitee.orryx.utils.getMap
import taboolib.module.configuration.Configuration

abstract class AbstractCastSkillLoader(final override val key: String, configuration: Configuration): AbstractSkillLoader(key, configuration), ICastSkill {

    override val actions: String = configuration.getString("Actions") ?: error("技能${key}位于${configuration.file}未书写Actions")

    override val extendActions: Map<String, String> = configuration.getMap("ExtendActions")

    override val castCheckAction: String? = options.getString("CastCheckAction")
}
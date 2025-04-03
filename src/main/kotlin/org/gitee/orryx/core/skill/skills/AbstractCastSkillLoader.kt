package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.ICastSkill
import taboolib.module.configuration.Configuration

abstract class AbstractCastSkillLoader(final override val key: String, configuration: Configuration): AbstractSkillLoader(key, configuration), ICastSkill {

    override val actions: String = getCleanScript(configuration.getString("Actions") ?: error("技能${key}位于${configuration.file}未书写Actions"))

    override val castCheckAction: String? = options.getString("CastCheckAction")?.let { getCleanScript(it) }

    private fun getCleanScript(action: String): String {
        return action.split("\n").mapNotNull { if (it.trim().getOrNull(0) == '#') null else it }.joinToString("\n")
    }

}
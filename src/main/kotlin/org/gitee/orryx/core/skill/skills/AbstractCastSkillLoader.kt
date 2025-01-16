package org.gitee.orryx.core.skill.skills

import org.gitee.orryx.core.skill.ICastSkill
import taboolib.module.configuration.Configuration

abstract class AbstractCastSkillLoader(override val key: String, override val configuration: Configuration): AbstractSkillLoader(key, configuration), ICastSkill {

    override val actions: String
        get() = getScriptFactor(configuration.getString("Actions") ?: error("技能${key}位于${configuration.file}未书写Actions"))

    override val castCheckAction: String?
        get() = configuration.getString("CastCheckAction")?.let { getScriptFactor(it) }

    private fun getScriptFactor(action: String): String {
        return action.split("\n").mapNotNull { if (it.trim().getOrNull(0) == '#') null else it }.joinToString("\n")
    }

}
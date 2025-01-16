package org.gitee.orryx.core.skill

import org.gitee.orryx.api.OrryxAPI.ketherScriptLoader
import org.gitee.orryx.api.events.OrryxSkillReloadEvent
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.skills.*
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService
import taboolib.module.kether.printKetherErrorMessage

object SkillLoaderManager {

    private val skillMap by lazy { mutableMapOf<String, ISkill>() }

    internal fun getSkillLoader(key: String): ISkill? {
        return skillMap[key]
    }

    internal fun getSkills(): Map<String, ISkill> {
        return skillMap
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        if (OrryxSkillReloadEvent().call()) {
            skillMap.clear()
            files("skills", "操翻诸神拳.yml") { file ->
                val configuration = Configuration.loadFromFile(file)
                val type = (configuration.getString("Options.Type") ?: "Direct").uppercase()
                val skill = when(type) {
                    PASSIVE.uppercase() -> PassiveSkill(configuration.name, configuration)
                    DIRECT_AIM.uppercase() -> DirectAimSkill(configuration.name, configuration)
                    DIRECT.uppercase() -> DirectSkill(configuration.name, configuration)
                    PRESSING_AIM.uppercase() -> PressingAimSkill(configuration.name, configuration)
                    PRESSING.uppercase() -> PressingSkill(configuration.name, configuration)
                    else -> return@files
                }
                skillMap[skill.key] = skill
            }
            info("&e┣&7Skills loaded &e${skillMap.size} &a√".colored())
        }
    }

    internal fun loadScript(skill: ICastSkill): Script? {
        return try {
            ketherScriptLoader.load(ScriptService, skill.key, getBytes(skill.actions), namespaces)
        } catch (ex: Exception) {
            ex.printKetherErrorMessage()
            warning("Skill: ${skill.key}")
            null
        }
    }

}
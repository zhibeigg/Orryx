package org.gitee.orryx.core.skill

import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.OrryxSkillReloadEvent
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.skill.skills.*
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService

object SkillLoaderManager {

    private val skillMap by unsafeLazy { hashMapOf<String, ISkill>() }

    internal fun getSkillLoader(key: String): ISkill {
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
            val castSkillMap = hashMapOf<String, ICastSkill>()
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
                if (skill is ICastSkill) castSkillMap[skill.key] = skill
            }
            consoleMessage("&e┣&7Skills loaded &e${skillMap.size} &a√")
            StateManager.reload(castSkillMap)
        }
    }

    internal fun loadScript(skill: ICastSkill): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, skill.key, getBytes(skill.actions), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            warning("Skill: ${skill.key} 主Action加载失败")
            ex.printKetherErrorMessage()
            null
        }
    }

    internal fun loadExtendScript(skill: ICastSkill): Map<String, Script?> {
        return skill.extendActions.mapValues {
            try {
                OrryxAPI.ketherScriptLoader.load(ScriptService, "${skill.key}@${it.key}", getBytes(it.value), orryxEnvironmentNamespaces)
            } catch (ex: Exception) {
                warning("Skill: ${skill.key} ExtendAction: ${it.key} 加载失败")
                ex.printKetherErrorMessage()
                null
            }
        }
    }
}
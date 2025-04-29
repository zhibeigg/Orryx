package org.gitee.orryx.core.kether

import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.debug
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submitAsync
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.extend

/**
 * @author zhibei
 * @param skill 技能名
 * @throws IllegalStateException KetherScript初始化时检测到不存在的技能
 * @suppress 使用未注册的skill名
 * */
class KetherScript(val skill: String, override val script: Script): IKetherScript {

    init {
        if (SkillLoaderManager.getSkillLoader(skill) == null) error("KetherScript初始化时检测到不存在的技能$skill")
    }

    override fun runActions(skillParameter: SkillParameter, map: Map<String, Any>?) {
        pluginScope.launch {
            debug("run skill action $map")
            val playerRunningSpace =
                ScriptManager.runningSkillScriptsMap.getOrPut(skillParameter.player.uniqueId) { PlayerRunningSpace(skillParameter.player) }

            var context: ScriptContext? = null
            ScriptManager.runScript(adaptPlayer(skillParameter.player), skillParameter, script) {
                playerRunningSpace.invoke(this, skill)
                map?.let { extend(it) }
                context = this
            }.whenComplete { _, _ ->
                playerRunningSpace.release(context!!, skill)
            }
        }
    }
}
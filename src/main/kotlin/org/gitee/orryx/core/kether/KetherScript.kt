package org.gitee.orryx.core.kether

import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.SkillLoaderManager
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.extend

/**
 * @author zhibei
 * @param skill 技能名
 * @param actions 脚本字符串
 * @throws IllegalStateException KetherScript初始化时检测到不存在的技能
 * @suppress 使用未注册的skill名
 * */
class KetherScript(val skill: String, override val script: Script): IKetherScript {

    init {
        if (SkillLoaderManager.getSkillLoader(skill) == null) error("KetherScript初始化时检测到不存在的技能$skill")
    }

    override fun runActions(skillParameter: SkillParameter, map: Map<String, Any>?) {
        val playerSkillsRunningSpace =
            ScriptManager.runningScriptsMap.getOrPut(skillParameter.player.uniqueId) { PlayerSkillsRunningSpace(skillParameter.player) }

        var context: ScriptContext? = null
        ScriptManager.runScript(adaptPlayer(skillParameter.player), skillParameter, script) {
            playerSkillsRunningSpace.invoke(this, this@KetherScript, skill)
            map?.let { extend(it) }
            context = this
        }.thenRun {
            playerSkillsRunningSpace.release(context!!, skill)
        }
    }

}
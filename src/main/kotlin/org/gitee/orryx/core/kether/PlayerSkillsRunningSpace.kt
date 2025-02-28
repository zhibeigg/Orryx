package org.gitee.orryx.core.kether

import org.bukkit.entity.Player
import taboolib.module.kether.ScriptContext

class PlayerSkillsRunningSpace(val player: Player) {

    private val skillRunningSpaceMap = mutableMapOf<String, SkillRunningSpace>()

    fun invoke(context: ScriptContext, ketherScript: KetherScript, skill: String) {
        val runningSpace = skillRunningSpaceMap[skill]
        if (runningSpace == null) {
            skillRunningSpaceMap[skill] = SkillRunningSpace(skill, ketherScript).apply { addScriptContext(context) }
        } else {
            runningSpace.addScriptContext(context)
        }
    }

    fun release(context: ScriptContext, skill: String) {
        val runningSpace = skillRunningSpaceMap[skill]
        if (runningSpace != null) {
            runningSpace.removeScriptContext(context)
            if (runningSpace.isEmpty()) {
                skillRunningSpaceMap.remove(skill)
            }
        }
    }

    fun terminateAll() {
        skillRunningSpaceMap.forEach {
            it.value.terminate()
        }
    }

    fun terminate(skill: String) {
        skillRunningSpaceMap[skill]?.terminate()
    }

}
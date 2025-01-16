package org.gitee.orryx.core.kether

import taboolib.module.kether.ScriptContext

class SkillRunningSpace(val skill: String, val ketherScript: IKetherScript) {

    private val runningScriptContexts by lazy { mutableListOf<ScriptContext>() }

    fun addScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts.add(scriptContext)
    }

    fun removeScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts.remove(scriptContext)
    }

    fun isEmpty(): Boolean {
        return runningScriptContexts.isEmpty()
    }

}
package org.gitee.orryx.core.kether

import org.gitee.orryx.api.events.OrryxScriptTerminateEvent
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService

class RunningSpace(val tag: String) {

    private val runningScriptContexts by lazy { hashMapOf<String, ScriptContext>() }

    fun addScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts[scriptContext.id] = scriptContext
    }

    fun removeScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts.remove(scriptContext.id)
    }

    fun isEmpty(): Boolean {
        return runningScriptContexts.isEmpty()
    }

    fun terminate() {
        if (OrryxScriptTerminateEvent.Pre(this).call()) {
            runningScriptContexts.forEach {
                val id = it.value.id
                ScriptService.terminateQuest(it.value)
                ScriptManager.cleanUp(id)
            }
            OrryxScriptTerminateEvent.Post(this).call()
            runningScriptContexts.clear()
        }
    }

    fun foreach(func: ScriptContext.() -> Unit) {
        runningScriptContexts.forEach { it.value.func() }
    }

}
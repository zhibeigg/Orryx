package org.gitee.orryx.core.kether

import org.gitee.orryx.api.events.OrryxScriptTerminateEvent
import taboolib.common.util.unsafeLazy
import taboolib.module.kether.ScriptContext

class RunningSpace(val tag: String) {

    private val runningScriptContexts by unsafeLazy { hashMapOf<String, ScriptContext>() }

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
                //此处顺序不可变
                ScriptManager.cleanUp(id)
                it.value.terminate()
            }
            OrryxScriptTerminateEvent.Post(this).call()
            runningScriptContexts.clear()
        }
    }

    fun foreach(func: ScriptContext.() -> Unit) {
        runningScriptContexts.forEach { it.value.func() }
    }

}
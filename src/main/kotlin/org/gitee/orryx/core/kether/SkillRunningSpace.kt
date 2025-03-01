package org.gitee.orryx.core.kether

import taboolib.module.kether.ScriptContext
import java.util.concurrent.LinkedBlockingDeque

class SkillRunningSpace(val skill: String, val ketherScript: IKetherScript) {

    private val runningScriptContexts by lazy { hashMapOf<String, Context>() }

    class Context(val scriptContext: ScriptContext) {

        private val closeables by lazy { LinkedBlockingDeque<AutoCloseable>() }

        fun addCloseable(closeable: AutoCloseable) {
            closeables.add(closeable)
        }

        fun terminate() {
            scriptContext.terminate()
            cleanUp()
        }

        fun cleanUp() {
            while (!closeables.isEmpty()) {
                try {
                    closeables.pollFirst().close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    fun addScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts[scriptContext.quest.id] = Context(scriptContext)
    }

    fun removeScriptContext(scriptContext: ScriptContext) {
        runningScriptContexts.remove(scriptContext.quest.id)
    }

    fun addCloseable(scriptContext: ScriptContext, closeable: AutoCloseable) {
        runningScriptContexts[scriptContext.quest.id]?.addCloseable(closeable)
    }

    fun isEmpty(): Boolean {
        return runningScriptContexts.isEmpty()
    }

    fun terminate() {
        runningScriptContexts.forEach {
            it.value.terminate()
        }
        runningScriptContexts.clear()
    }

}
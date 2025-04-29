package org.gitee.orryx.core.kether

import org.bukkit.entity.Player
import taboolib.module.kether.ScriptContext
import java.util.concurrent.ConcurrentHashMap

class PlayerRunningSpace(val player: Player) {

    private val runningSpaceMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<String, RunningSpace>() }

    fun invoke(context: ScriptContext, tag: String) {
        runningSpaceMap.getOrPut(tag) { RunningSpace(tag) }.addScriptContext(context)
    }

    fun release(context: ScriptContext, tag: String) {
        runningSpaceMap[tag]?.apply {
            removeScriptContext(context)
            if (isEmpty()) {
                runningSpaceMap.remove(tag)
            }
        }
        ScriptManager.cleanUp(context.id)
    }

    fun terminateAll() {
        runningSpaceMap.forEach {
            it.value.terminate()
        }
    }

    fun terminate(tag: String) {
        runningSpaceMap[tag]?.terminate()
    }
}
package org.gitee.orryx.core.kts

import kotlinx.coroutines.Job
import org.gitee.orryx.core.kts.script.ScriptState
import java.util.concurrent.ConcurrentHashMap

interface ScriptManager {

    val scripts: ConcurrentHashMap<String, ScriptState>

    fun compile(scriptName: String): Job

    fun load(scriptName: String)

    fun forceLoad(scriptName: String)

    fun isLoaded(scriptName: String): Boolean

    fun unload(scriptName: String)

    fun reload(scriptName: String)

    fun recompile(scriptName: String)

    fun hotRecompile(scriptName: String)

    fun disableHotRecompile(scriptName: String)

    fun isHotRecompileEnable(scriptName: String): Boolean

    fun discoveryAllScripts()
}
package org.gitee.orryx.core.kts

import kotlinx.coroutines.Job
import org.gitee.orryx.core.kts.script.ScriptState
import java.util.concurrent.ConcurrentHashMap

/**
 * KTS 脚本管理接口。
 *
 * @property scripts 脚本状态缓存
 */
interface ScriptManager {

    val scripts: ConcurrentHashMap<String, ScriptState>

    /**
     * 编译脚本。
     *
     * @param scriptName 脚本名
     * @return 编译任务
     */
    fun compile(scriptName: String): Job

    /**
     * 加载脚本。
     *
     * @param scriptName 脚本名
     */
    fun load(scriptName: String)

    /**
     * 强制加载脚本。
     *
     * @param scriptName 脚本名
     */
    fun forceLoad(scriptName: String)

    /**
     * 判断脚本是否已加载。
     *
     * @param scriptName 脚本名
     * @return 是否已加载
     */
    fun isLoaded(scriptName: String): Boolean

    /**
     * 卸载脚本。
     *
     * @param scriptName 脚本名
     */
    fun unload(scriptName: String)

    /**
     * 重载脚本。
     *
     * @param scriptName 脚本名
     */
    fun reload(scriptName: String)

    /**
     * 重新编译脚本。
     *
     * @param scriptName 脚本名
     */
    fun recompile(scriptName: String)

    /**
     * 热重编译脚本。
     *
     * @param scriptName 脚本名
     */
    fun hotRecompile(scriptName: String)

    /**
     * 禁用脚本热重编译。
     *
     * @param scriptName 脚本名
     */
    fun disableHotRecompile(scriptName: String)

    /**
     * 是否启用脚本热重编译。
     *
     * @param scriptName 脚本名
     * @return 是否启用
     */
    fun isHotRecompileEnable(scriptName: String): Boolean

    /**
     * 发现并加载所有脚本。
     */
    fun discoveryAllScripts()
}

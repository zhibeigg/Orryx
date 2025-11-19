package org.gitee.orryx.core.kts

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.reload.Reload
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.platform.util.bukkitPlugin
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.script.experimental.jvm.util.isError

/**
 * KTS 脚本工具类
 * 提供便捷的脚本操作方法
 */
object KtsScriptUtils {

    @Reload(1)
    private fun reload() {
        OrryxAPI.ioScope.launch {
            defaultManager?.reloadAll()
        }
    }

    @Awake(LifeCycle.ENABLE)
    private fun enable() {
        KtsScriptCompiler.clearCache()
        val manager = KtsScriptManager(bukkitPlugin, getDataFolder())
        setDefaultManager(manager)
        OrryxAPI.ioScope.launch {
            manager.loadAll()
        }
        manager.autoReload = true
    }

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        defaultManager?.shutdown()
    }

    /**
     * 快速执行脚本文件
     *
     * @param file 脚本文件
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    suspend fun executeFile(
        file: File,
        player: Player? = null,
        variables: Map<String, Any> = emptyMap(),
        onSuccess: ((Any?) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val manager = getDefaultManager()
        val scriptName = file.nameWithoutExtension

        // 确保脚本已加载
        if (!manager.isLoaded(scriptName)) {
            if (!manager.load(file)) {
                onFailure?.invoke("脚本加载失败")
                return
            }
        }

        when (val result = manager.execute(scriptName, player, variables)) {
            is KtsExecutionResult.Success -> onSuccess?.invoke(result.value)
            is KtsExecutionResult.Failure -> onFailure?.invoke(result.message)
        }
    }

    /**
     * 异步执行脚本文件
     *
     * @param file 脚本文件
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @return CompletableFuture
     */
    fun executeFileAsync(
        file: File,
        player: Player? = null,
        variables: Map<String, Any> = emptyMap()
    ): CompletableFuture<KtsExecutionResult> {
        val future = CompletableFuture<KtsExecutionResult>()

        OrryxAPI.pluginScope.launch {
            try {
                val manager = getDefaultManager()
                val scriptName = file.nameWithoutExtension

                // 确保脚本已加载
                if (!manager.isLoaded(scriptName)) {
                    if (!manager.load(file)) {
                        future.complete(KtsExecutionResult.failure("脚本加载失败"))
                        return@launch
                    }
                }

                val result = manager.execute(scriptName, player, variables)
                future.complete(result)
            } catch (e: Exception) {
                future.complete(KtsExecutionResult.failure("执行异常: ${e.message}"))
            }
        }

        return future
    }

    /**
     * 快速执行脚本代码
     *
     * @param code 脚本代码
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    suspend fun executeCode(
        code: String,
        player: Player? = null,
        variables: Map<String, Any> = emptyMap(),
        onSuccess: ((Any?) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        val manager = getDefaultManager()
        when (val result = manager.executeCode(code, player = player, variables = variables)) {
            is KtsExecutionResult.Success -> onSuccess?.invoke(result.value)
            is KtsExecutionResult.Failure -> onFailure?.invoke(result.message)
        }
    }

    /**
     * 异步执行脚本代码
     *
     * @param code 脚本代码
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @return CompletableFuture
     */
    fun executeCodeAsync(
        code: String,
        player: Player? = null,
        variables: Map<String, Any> = emptyMap()
    ): CompletableFuture<KtsExecutionResult> {
        val future = CompletableFuture<KtsExecutionResult>()

        OrryxAPI.pluginScope.launch {
            try {
                val manager = getDefaultManager()
                val result = manager.executeCode(code, player = player, variables = variables)
                future.complete(result)
            } catch (e: Exception) {
                future.complete(KtsExecutionResult.failure("执行异常: ${e.message}"))
            }
        }

        return future
    }

    /**
     * 验证脚本语法
     *
     * @param file 脚本文件
     * @return 是否有效
     */
    suspend fun validateScript(file: File): Boolean {
        val result = KtsScriptCompiler.compile(file, useCache = false)
        if (result.isError()) {
            warning("脚本验证失败: ${file.name}")
            result.reports.forEach { diagnostic ->
                warning("  ${diagnostic.severity}: ${diagnostic.message}")
            }
            return false
        }
        return true
    }

    /**
     * 验证脚本代码语法
     *
     * @param code 脚本代码
     * @param name 脚本名称
     * @return 是否有效
     */
    suspend fun validateCode(code: String, name: String = "script"): Boolean {
        val result = KtsScriptCompiler.compileCode(code, name, useCache = false)
        if (result.isError()) {
            warning("脚本代码验证失败: $name")
            result.reports.forEach { diagnostic ->
                warning("  ${diagnostic.severity}: ${diagnostic.message}")
            }
            return false
        }
        return true
    }

    /**
     * 获取默认脚本管理器
     * 注意: 这里需要在实际使用时注入真实的管理器实例
     */
    fun getDefaultManager(): KtsScriptManager {
        return defaultManager ?: error("KTS 脚本管理器未初始化，请先调用 KtsScriptUtils.setDefaultManager()")
    }

    /**
     * 默认管理器实例
     */
    private var defaultManager: KtsScriptManager? = null

    /**
     * 设置默认管理器
     */
    fun setDefaultManager(manager: KtsScriptManager) {
        defaultManager = manager
        info("已设置默认 KTS 脚本管理器")
    }

    /**
     * 获取脚本缓存统计信息
     */
    fun getCacheStats(): String {
        val cacheSize = KtsScriptCompiler.getCacheSize()
        val loadedScripts = defaultManager?.getLoadedScripts()?.size ?: 0

        return """
            |KTS 脚本缓存统计:
            |  - 编译缓存: $cacheSize 个
            |  - 已加载脚本: $loadedScripts 个
        """.trimMargin()
    }

    /**
     * 清空所有缓存
     */
    fun clearAllCaches() {
        KtsScriptCompiler.clearCache()
        KtsScriptContext.clearGlobal()
        info("已清空所有 KTS 脚本缓存")
    }
}

/**
 * 脚本执行结果扩展函数
 */

/**
 * 当执行成功时
 */
inline fun KtsExecutionResult.onSuccess(action: (Any?) -> Unit): KtsExecutionResult {
    if (this is KtsExecutionResult.Success) {
        action(value)
    }
    return this
}

/**
 * 当执行失败时
 */
inline fun KtsExecutionResult.onFailure(action: (String) -> Unit): KtsExecutionResult {
    if (this is KtsExecutionResult.Failure) {
        action(message)
    }
    return this
}

/**
 * 转换返回值类型
 */
inline fun <reified T> KtsExecutionResult.getAs(): T? {
    return when (this) {
        is KtsExecutionResult.Success -> value as? T
        is KtsExecutionResult.Failure -> null
    }
}

/**
 * 转换返回值类型(带默认值)
 */
inline fun <reified T> KtsExecutionResult.getAsOrDefault(default: T): T {
    return getAs() ?: default
}

/**
 * Player 扩展函数 - 执行脚本
 */
suspend fun Player.executeKtsScript(
    scriptName: String,
    variables: Map<String, Any> = emptyMap()
): KtsExecutionResult {
    return KtsScriptUtils.getDefaultManager().execute(scriptName, this, variables)
}

/**
 * Player 扩展函数 - 执行脚本代码
 */
suspend fun Player.executeKtsCode(
    code: String,
    variables: Map<String, Any> = emptyMap()
): KtsExecutionResult {
    return KtsScriptUtils.getDefaultManager().executeCode(code, player = this, variables = variables)
}

package org.gitee.orryx.core.kts

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.gitee.orryx.api.OrryxAPI
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.script.experimental.jvm.util.isError

/**
 * KTS 脚本管理器
 * 负责脚本的加载、缓存、热重载和生命周期管理
 */
class KtsScriptManager(
    private val plugin: Plugin,
    private val scriptsDirectory: File
) {

    /**
     * 脚本执行器
     */
    private val executor = KtsScriptExecutor(plugin)

    /**
     * 已加载的脚本 (脚本名称 -> 脚本文件)
     */
    private val loadedScripts = ConcurrentHashMap<String, File>()

    /**
     * 脚本文件监听器
     */
    private var watchService: WatchService? = null

    /**
     * 文件监听线程
     */
    private val watchExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "KtsScriptWatcher").apply { isDaemon = true }
    }

    /**
     * 是否启用自动重载
     */
    var autoReload: Boolean = false
        set(value) {
            field = value
            if (value) {
                startWatching()
            } else {
                stopWatching()
            }
        }

    /**
     * 初始化管理器
     */
    init {
        // 确保脚本目录存在
        if (!scriptsDirectory.exists()) {
            scriptsDirectory.mkdirs()
        }
    }

    /**
     * 加载所有脚本
     *
     * @param extension 脚本文件扩展名
     * @return 加载成功的脚本数量
     */
    suspend fun loadAll(extension: String = "kts"): Int {
        if (!scriptsDirectory.exists()) {
            warning("脚本目录不存在: ${scriptsDirectory.path}")
            return 0
        }

        var count = 0
        scriptsDirectory.listFiles { file ->
            file.extension == extension
        }?.forEach { file ->
            if (load(file)) {
                count++
            }
        }

        info("已加载 $count 个脚本")
        return count
    }

    /**
     * 加载单个脚本
     *
     * @param file 脚本文件
     * @return 是否加载成功
     */
    suspend fun load(file: File): Boolean {
        if (!file.exists()) {
            warning("脚本文件不存在: ${file.path}")
            return false
        }

        // 尝试编译脚本以验证语法
        val compilationResult = KtsScriptCompiler.compile(file)
        if (compilationResult.isError()) {
            warning("脚本编译失败: ${file.name}")
            compilationResult.reports.forEach { diagnostic ->
                warning("  ${diagnostic.severity}: ${diagnostic.message}")
            }
            return false
        }

        val scriptName = file.nameWithoutExtension
        loadedScripts[scriptName] = file
        info("已加载脚本: $scriptName")
        return true
    }

    /**
     * 重载单个脚本
     *
     * @param scriptName 脚本名称
     * @return 是否重载成功
     */
    suspend fun reload(scriptName: String): Boolean {
        val file = loadedScripts[scriptName]
        if (file == null) {
            warning("脚本未加载: $scriptName")
            return false
        }

        // 清除缓存
        KtsScriptCompiler.clearCache(file)

        // 重新加载
        return load(file)
    }

    /**
     * 重载所有脚本
     *
     * @return 重载成功的脚本数量
     */
    suspend fun reloadAll(): Int {
        info("正在重载所有脚本...")
        KtsScriptCompiler.clearCache()

        var count = 0
        loadedScripts.keys.toList().forEach { scriptName ->
            if (reload(scriptName)) {
                count++
            }
        }

        info("已重载 $count 个脚本")
        return count
    }

    /**
     * 卸载单个脚本
     *
     * @param scriptName 脚本名称
     * @return 是否卸载成功
     */
    fun unload(scriptName: String): Boolean {
        val file = loadedScripts.remove(scriptName)
        if (file == null) {
            warning("脚本未加载: $scriptName")
            return false
        }

        KtsScriptCompiler.clearCache(file)
        info("已卸载脚本: $scriptName")
        return true
    }

    /**
     * 卸载所有脚本
     */
    fun unloadAll() {
        info("正在卸载所有脚本...")
        val count = loadedScripts.size
        loadedScripts.clear()
        KtsScriptCompiler.clearCache()
        info("已卸载 $count 个脚本")
    }

    /**
     * 执行脚本
     *
     * @param scriptName 脚本名称
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @param timeout 超时时间(毫秒)
     * @return 执行结果
     */
    suspend fun execute(
        scriptName: String,
        player: Player? = null,
        variables: Map<String, Any> = emptyMap(),
        timeout: Long = 0
    ): KtsExecutionResult {
        val file = loadedScripts[scriptName] ?: return KtsExecutionResult.failure("脚本未加载: $scriptName")

        // 创建上下文
        val context = KtsScriptContext(plugin, player)
        context.putAll(variables)

        return executor.execute(file, context, timeout)
    }

    /**
     * 执行脚本代码
     *
     * @param code 脚本代码
     * @param name 脚本名称
     * @param player 执行脚本的玩家
     * @param variables 初始变量
     * @param timeout 超时时间(毫秒)
     * @return 执行结果
     */
    suspend fun executeCode(
        code: String,
        name: String = "inline",
        player: Player? = null,
        variables: Map<String, Any> = emptyMap(),
        timeout: Long = 0
    ): KtsExecutionResult {
        // 创建上下文
        val context = KtsScriptContext(plugin, player)
        context.putAll(variables)

        return executor.executeCode(code, name, context, timeout)
    }

    /**
     * 获取已加载的脚本列表
     */
    fun getLoadedScripts(): List<String> {
        return loadedScripts.keys.toList()
    }

    /**
     * 检查脚本是否已加载
     */
    fun isLoaded(scriptName: String): Boolean {
        return loadedScripts.containsKey(scriptName)
    }

    /**
     * 获取脚本文件
     */
    fun getScriptFile(scriptName: String): File? {
        return loadedScripts[scriptName]
    }

    /**
     * 启动文件监听
     */
    private fun startWatching() {
        if (watchService != null) {
            return
        }

        try {
            watchService = FileSystems.getDefault().newWatchService()
            val path = scriptsDirectory.toPath()

            path.register(
                watchService!!,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )

            watchExecutor.submit {
                watchForChanges()
            }

            info("已启动脚本文件监听")
        } catch (e: Exception) {
            warning("启动文件监听失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 停止文件监听
     */
    private fun stopWatching() {
        watchService?.close()
        watchService = null
        info("已停止脚本文件监听")
    }

    /**
     * 监听文件变化
     */
    private fun watchForChanges() {
        val service = watchService ?: return

        while (watchService != null) {
            try {
                val key = service.poll(1, TimeUnit.SECONDS) ?: continue

                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    val filename = event.context() as? Path ?: continue

                    if (filename.toString().endsWith(".kts")) {
                        val scriptName = filename.toString().removeSuffix(".kts")

                        when (kind) {
                            StandardWatchEventKinds.ENTRY_CREATE -> {
                                info("检测到新脚本: $scriptName")
                                val file = scriptsDirectory.resolve(filename.toString())

                                OrryxAPI.ioScope.launch {
                                    load(file)
                                }
                            }
                            StandardWatchEventKinds.ENTRY_MODIFY -> {
                                info("检测到脚本修改: $scriptName")

                                OrryxAPI.ioScope.launch {
                                    reload(scriptName)
                                }
                            }
                            StandardWatchEventKinds.ENTRY_DELETE -> {
                                info("检测到脚本删除: $scriptName")
                                unload(scriptName)
                            }
                        }
                    }
                }

                key.reset()
            } catch (e: Exception) {
                if (watchService != null) {
                    warning("文件监听异常: ${e.message}")
                }
            }
        }
    }

    /**
     * 关闭管理器
     */
    fun shutdown() {
        stopWatching()
        watchExecutor.shutdown()
        unloadAll()
        info("脚本管理器已关闭")
    }
}

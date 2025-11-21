package org.gitee.orryx.core.kts

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.kts.compiler.KtsScriptCompilerImpl
import org.gitee.orryx.core.kts.loader.KtsScriptLoaderImpl
import org.gitee.orryx.core.kts.script.KTS_EXTENSION
import org.gitee.orryx.core.kts.script.ScriptState
import org.gitee.orryx.core.kts.watcher.watchFolder
import org.gitee.orryx.utils.disable
import org.gitee.orryx.utils.isKtsScript
import org.gitee.orryx.utils.ktsNameRelative
import org.gitee.orryx.utils.minecraftMain
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.platform.BukkitPlugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * 脚本管理器实现
 *
 * 负责脚本的发现、编译、加载、卸载、热重载等核心功能
 */
class ScriptManagerImpl : ScriptManager {

    companion object {
        // 重新编译前的最小修改时间（秒）- 防抖延迟
        const val MINIMUM_MODIFY_TIME_TO_RECOMPILE_SECONDS = 1

        val plugin by unsafeLazy { BukkitPlugin.getInstance() }
    }

    // 所有已发现的脚本及其状态
    override val scripts: ConcurrentHashMap<String, ScriptState> = ConcurrentHashMap()

    // 脚本目录
    private val scriptDir by lazy { File(getDataFolder(), "scripts").apply { mkdirs() } }

    // 编译缓存目录
    private val cacheDir by lazy { File(getDataFolder(), "cache").apply { mkdirs() } }

    // 脚本编译器
    private val compiler by lazy { KtsScriptCompilerImpl(scriptDir, cacheDir) }

    // 脚本加载器
    private val loader by lazy {
        KtsScriptLoaderImpl(
            plugin,
            scriptDir,
            plugin::class.java.classLoader,
            ::getClassByName
        )
    }

    // 启用热重新编译的脚本集合
    private val hotrecompileScripts = ConcurrentSkipListSet<String>()

    // 重新编译队列：脚本名 -> 最后修改时间
    private val recompileQueue: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    /**
     * 插件启用时初始化
     * 1. 设置 Maven 缓存目录
     * 2. 发现所有脚本文件
     * 3. 编译所有脚本
     * 4. 加载所有未加载的脚本
     * 5. 设置热重新编译监视器
     */
    fun onPluginEnable() {
        // 重要：设置 Maven 仓库缓存目录到插件文件夹
        System.setProperty("user.home", File(CACHE_FOLDER).absolutePath)

        discoveryAllScripts()

        // 在第一个 tick 中加载脚本
        OrryxAPI.pluginScope.launch(Dispatchers.minecraftMain) {
            compileAll().joinAll()

            loadAllUnloaded()
        }

        setupHotRecompiler()
    }

    /**
     * 插件禁用时卸载所有脚本
     */
    fun onPluginDisable() {
        unloadAll()
    }

    /**
     * 编译指定的脚本
     * @param scriptName 脚本名称（不含扩展名）
     * @return 编译任务的 Job
     * @throws ScriptNotFoundException 如果脚本不存在
     * @throws ScriptInvalidStateException 如果脚本状态不允许编译
     */
    override fun compile(scriptName: String): Job {
        var scriptState = scripts[scriptName]

        // 如果脚本尚未发现，尝试发现它
        if (scriptState == null) {
            discoveryScript(scriptName)

            scriptState = scripts[scriptName]
        }

        if (scriptState == null) {
            throw ScriptNotFoundException("Could not compile the script $scriptName because was not found", scriptName)
        }

        // 如果状态不是已发现状态，可能已经加载或卸载
        if (
            scriptState !is ScriptState.Discovered &&
            scriptState !is ScriptState.CompileFail &&
            scriptState !is ScriptState.LoadFail
        ) {
            throw ScriptInvalidStateException("The script is currently is unloaded or loaded and could not compile again.", scriptState::class.java.simpleName)
        }

        return OrryxAPI.pluginScope.launch(Dispatchers.Default) {

            val scriptFile = File(scriptDir, "$scriptName.$KTS_EXTENSION")

            val cachedScript = compiler.getCachedScript(scriptFile)

            scripts[scriptName] = ScriptState.CheckingCache(scriptName)

            // 如果有有效的缓存，直接使用缓存
            val compiled = if (cachedScript != null && cachedScript.isValid) {
                cachedScript.compiled
            } else {
                // 没有缓存或缓存无效，重新编译
                val description = compiler.retrieveDescriptor(scriptFile)
                    ?: throw RetrieveScriptDefinitionException("Could not retrieve the script $scriptName informations.", scriptName)

                scripts[scriptName] = ScriptState.Compiling(scriptName, scriptFile, description)

                runCatching { compiler.compile(scriptFile, description) }
                    .getOrElse {
                        scripts[scriptName] = ScriptState.CompileFail(scriptName, it.toString())

                        return@launch
                    }
            }

            scripts[scriptName] = ScriptState.Unloaded(scriptName, compiled)
        }
    }

    /**
     * 加载指定的脚本
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     * @throws ScriptInvalidStateException 如果脚本状态不是 Unloaded
     */
    override fun load(scriptName: String) {
        val state = scripts[scriptName] ?: throw ScriptNotFoundException("Could not load the script $scriptName because it was not found.", scriptName)
        val unloaded = (state as? ScriptState.Unloaded) ?: throw ScriptInvalidStateException("Could not load the script because the current state of the script is not unloaded.", scriptName)

        load(unloaded)
    }

    private fun load(unloaded: ScriptState.Unloaded) {
        val scriptName = unloaded.scriptName

        scripts[scriptName] = ScriptState.Loading(scriptName, unloaded.compiledScript)

        val loaded = runBlocking {
            runCatching { loader.load(unloaded.compiledScript) }
                .getOrElse {
                    scripts[scriptName] = ScriptState.LoadFail(scriptName, it.toString())

                    null
                }
        } ?: return

        scripts[scriptName] = ScriptState.Loaded(scriptName, loaded)
    }

    override fun forceLoad(scriptName: String) {
        val currentState = scripts[scriptName]
            ?: throw ScriptNotFoundException("Could not load the script $scriptName because it was not found.", scriptName)

        when (currentState) {
            is ScriptState.Loaded,
            is ScriptState.Unloaded,
            -> load(scriptName)
            is ScriptState.Discovered,
            is ScriptState.CompileFail,
            is ScriptState.LoadFail,
            -> recompile(scriptName)
            else -> {}
        }
    }

    private fun compileAll(): List<Job> {
        info("Compiling all discovered scripts")
        return scripts.keys.map { compile(it) }
    }

    private fun loadAllUnloaded() {
        info("Loading all unloaded scripts")
        for ((scriptName, state) in scripts) {
            if (state is ScriptState.Unloaded) {
                load(state)
            }
        }
    }

    override fun isLoaded(scriptName: String): Boolean {
        // TODO: use lower case or something?
        return (scripts[scriptName] as? ScriptState.Loaded?) != null
    }

    /**
     * 卸载指定的脚本
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     * @throws ScriptInvalidStateException 如果脚本未加载
     */
    override fun unload(scriptName: String) {
        val script = scripts[scriptName] ?: throw ScriptNotFoundException("Could not unload the script $scriptName bacause was not found.", scriptName)
        val loaded = script as? ScriptState.Loaded ?: throw ScriptInvalidStateException("Could not unload the script $scriptName because it is not loaded.", scriptName)

        unload(loaded)
    }

    private fun unload(loaded: ScriptState.Loaded) {
        val scriptName = loaded.scriptName

        loaded.loadedScript.disable()

        scripts[scriptName] = ScriptState.Unloaded(scriptName, loaded.loadedScript.compiledScript)
    }

    private fun unloadAll() {
        val loadedScripts = scripts.values.filterIsInstance<ScriptState.Loaded>()

        info("Unloading all loaded scripts: ${loadedScripts.joinToString { it.scriptName }}")

        for (loaded in loadedScripts) {
            unload(loaded)
        }
    }

    /**
     * 重新加载脚本
     * 先卸载再加载，不重新编译
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     * @throws ScriptInvalidStateException 如果脚本未加载
     */
    override fun reload(scriptName: String) {
        val script = scripts[scriptName] ?: throw ScriptNotFoundException("Could not reload the script $scriptName because it was not found.", scriptName)
        val loaded = script as? ScriptState.Loaded ?: throw ScriptInvalidStateException("Could not reload the script $scriptName because it is not loaded.", scriptName)

        unload(loaded)

        val unloaded = scripts[scriptName] as? ScriptState.Unloaded
            ?: throw ScriptInvalidStateException("Could not complete reload the script $scriptName by loading it because was not found the script into the Unloaded state.", scriptName)

        load(unloaded)
    }

    /**
     * 重新编译并加载脚本
     * 如果脚本已加载，先卸载，然后重新编译并加载
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     */
    override fun recompile(scriptName: String) {
        val state = scripts[scriptName] ?: throw ScriptNotFoundException("Could not recompile the script $scriptName because it was not found.", scriptName)

        if (state is ScriptState.Loaded) {
            unload(scriptName)
        }

        // setting a discovered because the compile uses just discovered state.
        scripts[scriptName] = ScriptState.Discovered(scriptName)

        OrryxAPI.pluginScope.launch(Dispatchers.minecraftMain) {
            compile(scriptName).join()

            load(scriptName)
        }
    }

    /**
     * 启用指定脚本的热重新编译
     * 当脚本文件被修改时，会自动重新编译并加载
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     */
    override fun hotRecompile(scriptName: String) {
        if (!scripts.containsKey(scriptName)) {
            throw ScriptNotFoundException("Could not enable the hot recompilation for a unknown script.", scriptName)
        }

        hotrecompileScripts.add(scriptName)
    }

    /**
     * 禁用指定脚本的热重新编译
     * @param scriptName 脚本名称
     * @throws ScriptNotFoundException 如果脚本不存在
     */
    override fun disableHotRecompile(scriptName: String) {
        if (!scripts.containsKey(scriptName)) {
            throw ScriptNotFoundException("Could not enable the hot recompilation for a unknown script.", scriptName)
        }

        hotrecompileScripts.remove(scriptName)
    }

    override fun isHotRecompileEnable(scriptName: String): Boolean {
        return scriptName in hotrecompileScripts
    }

    // 发现脚本文件夹中的所有脚本文件并添加到脚本列表
    override fun discoveryAllScripts() {
        for (scriptName in listScriptsFromFolder()) {
            if (!scripts.containsKey(scriptName)) {
                scripts[scriptName] = ScriptState.Discovered(scriptName)
            }
        }
    }

    private fun discoveryScript(scriptName: String) {
        if (scripts.containsKey(scriptName)) {
            throw ScriptInvalidStateException("The script $scriptName was already discovered.", scriptName)
        }

        val scriptFile = File(scriptDir, "$scriptName.$KTS_EXTENSION")

        if (scriptFile.exists()) {
            scripts[scriptName] = ScriptState.Discovered(scriptName)
        } else {
            throw ScriptFileDoesNotExistException("Could not find the script file by the name $scriptName!", scriptName, scriptFile)
        }
    }

    private fun getClassByName(name: String): Class<*>? {
        for (value in scripts.values.filterIsInstance<ScriptState.Loaded>()) {
            val findClass = value.loadedScript.classLoader.findClass(name, false)

            if (findClass != null) {
                return findClass
            }
        }

        return null
    }

    private fun listScriptsFromFolder(): Set<String> {
        return scriptDir.walkTopDown()
            .filter { it.isKtsScript }
            .map { it.ktsNameRelative(scriptDir) }
            .toSet()
    }

    /**
     * 设置热重新编译系统
     * 1. 启动一个定时任务，处理重新编译队列（带防抖）
     * 2. 监视脚本文件夹，当文件被修改时加入重新编译队列
     */
    private fun setupHotRecompiler() {
        OrryxAPI.pluginScope.launch(Dispatchers.minecraftMain) {
            while (true) {
                for ((script, lastTimeModified) in recompileQueue) {
                    if (System.currentTimeMillis() - lastTimeModified > MINIMUM_MODIFY_TIME_TO_RECOMPILE_SECONDS * 1000) {
                        recompileQueue.remove(script)
                        try {
                            recompile(script)
                        } catch (e: Throwable) {
                            // ignore any recompilation error to not broken your timer
                            e.printStackTrace()
                        }
                    }
                }
                delay(300)
            }
        }

        watchFolder(scriptDir.toPath())
            .onEach {
                val scriptName = it.file.ktsNameRelative(scriptDir)

                if (scriptName in hotrecompileScripts) {
                    recompileQueue[scriptName] = System.currentTimeMillis()
                }
            }
            .launchIn(OrryxAPI.pluginScope)
    }
}

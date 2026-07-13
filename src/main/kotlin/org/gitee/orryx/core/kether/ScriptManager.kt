package org.gitee.orryx.core.kether

import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.OrryxAPI.Companion.ketherScriptLoader
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.common.NanoId
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import org.gitee.orryx.utils.printKetherErrorMessage
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.module.kether.*
import taboolib.module.kether.KetherFunction.reader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object ScriptManager {

    val runningSkillScriptsMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<UUID, PlayerRunningSpace>() }
    val runningStationScriptsMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<UUID, PlayerRunningSpace>() }

    val wikiActions by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Action>() }
    val wikiSelectors by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Selector>() }
    val wikiTriggers by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Trigger>() }
    val wikiProperties by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Property>() }

    private val scriptCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build<String, Script>()
    }
    private val resourceScopeLock = Any()
    private val resourceScopes = WeakHashMap<ScriptContext, ResourceScope>()
    private val activeResourceScopes = ConcurrentHashMap<String, ResourceScope>()
    private val acceptingResources = AtomicBoolean(true)

    fun terminateAllSkills() {
        runningSkillScriptsMap.forEach {
            it.value.terminateAll()
        }
    }
    fun terminateAllStation() {
        runningStationScriptsMap.forEach {
            it.value.terminateAll()
        }
    }

    @SubscribeEvent(EventPriority.MONITOR)
    private fun quit(e: PlayerQuitEvent) {
        runningSkillScriptsMap.remove(e.player.uniqueId)?.terminateAll()
        runningStationScriptsMap.remove(e.player.uniqueId)?.terminateAll()
    }

    /**
     * 插件关闭时清理所有未关闭的资源
     */
    @Awake(LifeCycle.DISABLE)
    private fun onDisable() {
        acceptingResources.set(false)
        activeResourceScopes.values.toSet().forEach(ResourceScope::close)
        activeResourceScopes.clear()
    }

    internal fun cleanUp(id: String) {
        activeResourceScopes.remove(id)?.close()
    }

    internal fun cleanUp(context: ScriptContext) {
        val scope = synchronized(resourceScopeLock) {
            resourceScopes.getOrPut(context) { ResourceScope() }
        }
        activeResourceScopes.remove(context.id, scope)
        scope.close()
    }

    fun ScriptFrame.addOrryxCloseable(future: CompletableFuture<*>, closeable: AutoCloseable) {
        val context = script()
        val closeableId = NanoId.generate()
        if (!acceptingResources.get()) {
            runCatching { closeable.close() }.onFailure(Throwable::printStackTrace)
            return
        }
        val scope = synchronized(resourceScopeLock) {
            resourceScopes.getOrPut(context) { ResourceScope() }
        }
        if (scope.add(closeableId, closeable)) {
            activeResourceScopes[context.id] = scope
            if (!acceptingResources.get()) scope.close()
            if (scope.isClosed()) activeResourceScopes.remove(context.id, scope)
        }
        future.whenComplete { _, _ ->
            if (scope.remove(closeableId, closeable) && future.isCancelled) {
                runCatching { closeable.close() }.onFailure(Throwable::printStackTrace)
            }
            if (scope.isEmpty()) activeResourceScopes.remove(context.id, scope)
        }
    }

    internal fun ScriptContext.removeCloseable(): ConcurrentMap<String, AutoCloseable>? {
        val scope = synchronized(resourceScopeLock) { resourceScopes[this] } ?: return null
        activeResourceScopes.remove(id, scope)
        return scope.drain()
    }

    private class ResourceScope {
        private val closed = AtomicBoolean(false)
        private val resources = ConcurrentHashMap<String, AutoCloseable>()

        fun add(id: String, closeable: AutoCloseable): Boolean {
            if (closed.get()) {
                closeSafely(closeable)
                return false
            }
            resources[id] = closeable
            if (closed.get() && resources.remove(id, closeable)) {
                closeSafely(closeable)
                return false
            }
            return true
        }

        fun remove(id: String, closeable: AutoCloseable): Boolean {
            return resources.remove(id, closeable)
        }

        fun isEmpty(): Boolean = resources.isEmpty()

        fun isClosed(): Boolean = closed.get()

        fun drain(): ConcurrentMap<String, AutoCloseable> {
            closed.set(true)
            return ConcurrentHashMap(resources).also { resources.clear() }
        }

        fun close() {
            if (!closed.compareAndSet(false, true)) return
            while (true) {
                val snapshot = resources.entries.toList()
                if (snapshot.isEmpty()) return
                snapshot.forEach { (id, closeable) ->
                    if (resources.remove(id, closeable)) closeSafely(closeable)
                }
            }
        }

        private fun closeSafely(closeable: AutoCloseable) {
            runCatching { closeable.close() }.onFailure(Throwable::printStackTrace)
        }
    }

    @Reload(weight = 2)
    fun reload() {
        terminateAllSkills()
        terminateAllStation()
        scriptCache.invalidateAll()
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, script: Script, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        return try {
            ScriptContext.create(script).also {
                it.sender = sender
                it.id = NanoId.generate()
                it[PARAMETER] = parameter
                context?.invoke(it)
            }.runActions()
        } catch (throwable: Throwable) {
            throwable.printKetherErrorMessage(false)
            failedFuture(throwable)
        }
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, action: String, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        val uuid = NanoId.generate()
        val script = scriptCache.get(action) {
            runKether {
                ketherScriptLoader.load(
                    ScriptService,
                    "orryx_temp_${uuid}",
                    getBytes(action),
                    orryxEnvironmentNamespaces
                )
            }
        } ?: return failedFuture(IllegalStateException("Kether 脚本加载失败"))

        return try {
            ScriptContext.create(script).also {
                it.sender = sender
                it.id = uuid
                it[PARAMETER] = parameter
                context?.invoke(it)
            }.runActions()
        } catch (throwable: Throwable) {
            throwable.printKetherErrorMessage(false)
            failedFuture(throwable)
        }
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: String, context: (ScriptContext.() -> Unit)? = null): String {
        return reader.replaceNested(actions) {
            val uuid = NanoId.generate()
            val script = scriptCache.get(this) {
                runKether {
                    ketherScriptLoader.load(
                        ScriptService,
                        "orryx_temp_${uuid}",
                        getBytes(this),
                        orryxEnvironmentNamespaces
                    )
                }
            } ?: return@replaceNested "none-error"

            runKether("none-error") {
                val scriptContext = ScriptContext.create(script).also {
                    it.sender = sender
                    it.id = uuid
                    it[PARAMETER] = parameter
                    context?.invoke(it)
                }
                val execution = scriptContext.runActions()
                if (!execution.isDone) {
                    cleanUp(scriptContext)
                    scriptContext.terminate()
                    "none-error"
                } else {
                    try {
                        execution.getNow(null).toString()
                    } finally {
                        cleanUp(scriptContext)
                    }
                }
            }
        }
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: List<String>, context: (ScriptContext.() -> Unit)? = null): List<String> {
        return actions.map { parseScript(sender, parameter, it, context) }
    }

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    inline fun <T> runKether(el: T, detailError: Boolean = false, function: () -> T): T {
        try {
            return function()
        } catch (e: Exception) {
            if (e is IllegalStateException) warning(e.message)
            e.printKetherErrorMessage(detailError)
        }
        return el
    }

    inline fun <T> runKether(detailError: Boolean = false, function: () -> T): T? {
        try {
            return function()
        } catch (e: Exception) {
            if (e is IllegalStateException) warning(e.message)
            e.printKetherErrorMessage(detailError)
        }
        return null
    }
}
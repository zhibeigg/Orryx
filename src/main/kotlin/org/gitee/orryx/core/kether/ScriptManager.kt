package org.gitee.orryx.core.kether

import com.eatthepath.uuid.FastUUID
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.OrryxAPI.Companion.ketherScriptLoader
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import org.gitee.orryx.utils.printKetherErrorMessage
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

object ScriptManager {

    val runningSkillScriptsMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<UUID, PlayerRunningSpace>() }
    val runningStationScriptsMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ConcurrentHashMap<UUID, PlayerRunningSpace>() }

    val wikiActions by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Action>() }
    val wikiSelectors by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Selector>() }
    val wikiTriggers by unsafeLazy { mutableListOf<org.gitee.orryx.module.wiki.Trigger>() }

    private val scriptMap by unsafeLazy { ConcurrentHashMap<String, Script>() }
    private val closeableMap by unsafeLazy { hashMapOf<String, ConcurrentMap<UUID, AutoCloseable>>() }

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

    internal fun cleanUp(id: String) {
        val closeable = closeableMap.remove(id) ?: return
        val iterator = closeable.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            try {
                next.value.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun ScriptFrame.addOrryxCloseable(future: CompletableFuture<*>, closeable: AutoCloseable) {
        val uuid = UUID.randomUUID()
        script().addCloseable(uuid, closeable)
        future.whenComplete { _, _ ->
            closeableMap[script().id]?.apply {
                if (isNotEmpty()) {
                    remove(uuid)
                }
            }
        }
    }

    private fun ScriptContext.addCloseable(uuid: UUID, closeable: AutoCloseable) {
        closeableMap.getOrPut(id) { ConcurrentHashMap() }[uuid] = closeable
    }

    internal fun ScriptContext.removeCloseable(): ConcurrentMap<UUID, AutoCloseable>? {
        return closeableMap.remove(id)
    }

    @Reload(weight = 2)
    fun reload() {
        scriptMap.clear()
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, script: Script, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        return runKether(CompletableFuture.completedFuture(null)) {
            ScriptContext.create(script).also {
                context?.invoke(it)
                it.sender = sender
                it.id = FastUUID.toString(UUID.randomUUID())
                it[PARAMETER] = parameter
            }.runActions()
        }
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, action: String, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        val uuid = FastUUID.toString(UUID.randomUUID())
        val script = scriptMap.getOrPut(action) {
            runKether {
                ketherScriptLoader.load(
                    ScriptService,
                    "orryx_temp_${uuid}",
                    getBytes(action),
                    orryxEnvironmentNamespaces
                )
            }
        }

        return runKether(CompletableFuture.completedFuture(null)) {
            ScriptContext.create(script).also {
                context?.invoke(it)
                it.sender = sender
                it.id = uuid
                it[PARAMETER] = parameter
            }.runActions()
        }
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: String, context: (ScriptContext.() -> Unit)? = null): String {
        val uuid = FastUUID.toString(UUID.randomUUID())
        return reader.replaceNested(actions) {
            val script = scriptMap.getOrPut(this) {
                runKether {
                    ketherScriptLoader.load(
                        ScriptService,
                        "orryx_temp_${uuid}",
                        getBytes(this),
                        orryxEnvironmentNamespaces
                    )
                }
            }

            runKether("none-error") {
                ScriptContext.create(script).also {
                    context?.invoke(it)
                    it.sender = sender
                    it.id = uuid
                    it[PARAMETER] = parameter
                }.runActions().orNull().toString()
            }
        }
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: List<String>, context: (ScriptContext.() -> Unit)? = null): List<String> {
        return actions.map { parseScript(sender, parameter, it, context) }
    }

    fun <T> runKether(el: T, detailError: Boolean = false, function: () -> T): T {
        try {
            return function()
        } catch (e: Throwable) {
            if (e is IllegalStateException) warning(e.message)
            e.printKetherErrorMessage(detailError)
        }
        return el
    }

    fun <T> runKether(detailError: Boolean = false, function: () -> T): T? {
        try {
            return function()
        } catch (e: Throwable) {
            if (e is IllegalStateException) warning(e.message)
            e.printKetherErrorMessage(detailError)
        }
        return null
    }
}
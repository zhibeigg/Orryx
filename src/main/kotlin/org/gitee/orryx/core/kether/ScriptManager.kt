package org.gitee.orryx.core.kether

import com.mojang.datafixers.kinds.App
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import taboolib.library.kether.Parser.*
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object ScriptManager {

    val runningSkillScriptsMap by unsafeLazy { hashMapOf<UUID, PlayerRunningSpace>() }
    val runningStationScriptsMap by unsafeLazy { hashMapOf<UUID, PlayerRunningSpace>() }

    val wikiActions by unsafeLazy { mutableListOf<org.gitee.orryx.core.wiki.Action>() }
    val wikiSelectors by unsafeLazy { mutableListOf<org.gitee.orryx.core.wiki.Selector>() }
    val wikiTriggers by unsafeLazy { mutableListOf<org.gitee.orryx.core.wiki.Trigger>() }

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
        return ScriptContext.create(script).also {
            context?.invoke(it)
            it.sender = sender
            it.id = UUID.randomUUID().toString()
            it[PARAMETER] = parameter
        }.runActions()
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, action: String, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        val uuid = UUID.randomUUID().toString()
        val script = scriptMap.computeIfAbsent(action) {
            KetherScriptLoader().load(ScriptService, "orryx_temp_${uuid}", getBytes(it), orryxEnvironmentNamespaces)
        }
        return ScriptContext.create(script).also {
            context?.invoke(it)
            it.sender = sender
            it.id = uuid
            it[PARAMETER] = parameter
        }.runActions()
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: String, context: (ScriptContext.() -> Unit)? = null): String {
        return KetherFunction.parse(
            actions,
            ScriptOptions.builder().sandbox(false).namespace(namespace = orryxEnvironmentNamespaces).sender(sender = sender).context {
                context?.invoke(this)
                this[PARAMETER] = parameter
            }.build()
        )
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: List<String>, context: (ScriptContext.() -> Unit)? = null): List<String> {
        return KetherFunction.parse(
            actions,
            ScriptOptions.builder().sandbox(false).namespace(namespace = orryxEnvironmentNamespaces).sender(sender = sender).context {
                context?.invoke(this)
                this[PARAMETER] = parameter
            }.build()
        )
    }

    fun <T> scriptParser(actions: Array<org.gitee.orryx.core.wiki.Action>, resolve: (QuestReader) -> QuestAction<T>): ScriptActionParser<T> {
        wikiActions += actions
        return ScriptActionParser(resolve)
    }

    fun <T> combinationParser(action: org.gitee.orryx.core.wiki.Action, builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
        wikiActions += action
        return combinationParser(builder)
    }

    fun <T> combinationParser(builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
        val parser = build(builder(ParserHolder, instance()))
        return ScriptActionParser { parser.resolve<T>(this) }
    }

}
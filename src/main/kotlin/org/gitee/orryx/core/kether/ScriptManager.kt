package org.gitee.orryx.core.kether

import com.mojang.datafixers.kinds.App
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.PARAMETER
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.library.kether.Parser.*
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.database.Database
import taboolib.module.kether.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object ScriptManager {

    val runningScriptsMap by lazy { mutableMapOf<UUID, PlayerSkillsRunningSpace>() }
    val wikiActions by lazy { mutableMapOf<String, org.gitee.orryx.core.wiki.Action>() }
    private val scriptMap by lazy { ConcurrentHashMap<String, Script>() }

    init {
        Database.prepareClose {
            runningScriptsMap.forEach {
                it.value.terminateAll()
            }
        }
    }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        runningScriptsMap.remove(e.player.uniqueId)
    }

    @Reload(weight = 2)
    fun reload() {
        scriptMap.clear()
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, script: Script, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        return ScriptContext.create(script).also {
            it.sender = sender
            it.id = UUID.randomUUID().toString()
            it[PARAMETER] = parameter
            context?.invoke(it)
        }.runActions()
    }

    fun runScript(sender: ProxyCommandSender, parameter: IParameter, action: String, context: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
        val uuid = UUID.randomUUID().toString()
        val script = scriptMap.computeIfAbsent(action) {
            KetherScriptLoader().load(ScriptService, "orryx_temp_${uuid}", getBytes(it), orryxEnvironmentNamespaces)
        }
        return ScriptContext.create(script).also {
            it.sender = sender
            it.id = uuid
            it[PARAMETER] = parameter
            context?.invoke(it)
        }.runActions()
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: String, context: (ScriptContext.() -> Unit)? = null): String {
        return KetherFunction.parse(
            actions,
            ScriptOptions.builder().sandbox(false).namespace(namespace = orryxEnvironmentNamespaces).sender(sender = sender).context {
                this[PARAMETER] = parameter
                context?.invoke(this)
            }.build()
        )
    }

    fun parseScript(sender: ProxyCommandSender, parameter: IParameter, actions: List<String>, context: (ScriptContext.() -> Unit)? = null): List<String> {
        return KetherFunction.parse(
            actions,
            ScriptOptions.builder().sandbox(false).namespace(namespace = orryxEnvironmentNamespaces).sender(sender = sender).context {
                this[PARAMETER] = parameter
                context?.invoke(this)
            }.build()
        )
    }

    fun <T> scriptParser(actions: Array<org.gitee.orryx.core.wiki.Action>, resolve: (QuestReader) -> QuestAction<T>): ScriptActionParser<T> {
        actions.forEach { action ->
            wikiActions[action.name] = action
        }
        return ScriptActionParser(resolve)
    }

    fun <T> combinationParser(action: org.gitee.orryx.core.wiki.Action, builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
        wikiActions[action.name] = action
        return combinationParser(builder)
    }

    fun <T> combinationParser(builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
        val parser = build(builder(ParserHolder, instance()))
        return ScriptActionParser { parser.resolve<T>(this) }
    }


}
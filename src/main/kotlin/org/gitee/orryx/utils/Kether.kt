package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.Parser
import taboolib.library.kether.Parser.Action
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import taboolib.module.kether.ParserHolder.command
import taboolib.module.kether.ParserHolder.option
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

const val NAMESPACE = "Orryx"

val namespaces = listOf("kether", NAMESPACE)

internal fun getBytes(actions: String): ByteArray {
    val s = if (actions.startsWith("def ")) actions else "def main = { $actions }"
    val texts = s.split("\n")
    return texts.mapNotNull { if (it.trim().startsWith("#")) null else it }.joinToString("\n").toByteArray(
        StandardCharsets.UTF_8
    )
}

internal fun ScriptFrame.bukkitPlayer(): Player {
    return script().sender?.castSafely<Player>() ?: error("Orryx脚本中未找到玩家")
}

internal fun ScriptContext.bukkitPlayer(): Player {
    return sender?.castSafely<Player>() ?: error("Orryx脚本中未找到玩家")
}

internal fun ScriptFrame.self(): IContainer {
    return bukkitPlayer().readContainer(script())!!
}

internal fun QuestReader.nextHeadActionOrNull(array: Array<out String>): ParsedAction<*>? {
    return try {
        mark()
        expects(*array)
        this.nextParsedAction()
    } catch (e: Exception) {
        reset()
        null
    }
}

internal fun QuestReader.nextHeadAction(id: String): ParsedAction<*>? {
    return nextHeadActionOrNull(arrayOf(id))
}

internal fun QuestReader.nextHeadAction(id: String, def: Any): ParsedAction<*> {
    return nextHeadActionOrNull(arrayOf(id)) ?: literalAction(def)
}

internal fun QuestReader.nextTheyContainer(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("they"))
}

internal fun <T> ScriptFrame.container(container: ParsedAction<*>?, func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<Any>? {
    return if (container == null) {
        null
    } else {
        run(container).thenApply {
            func(it.readContainer(script()) ?: return@thenApply)
        }
    }
}


internal fun <T> ScriptFrame.containerOrSelf(container: ParsedAction<*>?, func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return if (container == null) {
        CompletableFuture.completedFuture(func(self()))
    } else {
        run(container).thenApply {
            func(it.readContainer(script()).orElse(self()))
        }
    }
}

internal fun vector(): Parser<Vector?> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { vector ->
                vector as? Vector
            }
        }
    }
}

internal fun theyContainer(optional: Boolean = false) = if (optional) {
    command("they", then = container()).option()
} else {
    command("they", then = container())
}

internal fun container(): Parser<IContainer?> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { container ->
                container.readContainer(it.script())
            }
        }
    }
}

internal fun ScriptContext.runSubScript(action: String, extend: Boolean, func: (ScriptContext.() -> Unit)? = null): CompletableFuture<Any?> {
    return ScriptManager.runScript(sender!!, getParameter(), action) {
        if (extend) {
            extend(this@runSubScript.rootFrame().deepVars())
        }
        func?.invoke(this)
    }
}

internal fun Player.eval(action: String, map: Map<String, Any>): CompletableFuture<Any?> {
    return adaptCommandSender(this).eval(action, map)
}

internal fun ProxyCommandSender.eval(action: String, map: Map<String, Any>): CompletableFuture<Any?> {
    return KetherShell.eval(action, ScriptOptions.builder().sender(this@eval).sandbox(true).namespace(namespaces).vars(map).build())
}

internal fun Player.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return adaptCommandSender(this).parse(actions, map)
}

internal fun Player.parse(action: String, map: Map<String, Any>): String {
    return adaptCommandSender(this).parse(action, map)
}

internal fun ProxyCommandSender.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return KetherFunction.parse(actions, ScriptOptions.builder().sender(this@parse).sandbox(true).namespace(namespaces).vars(map).build())
}

internal fun ProxyCommandSender.parse(action: String, map: Map<String, Any>): String {
    return KetherFunction.parse(action, ScriptOptions.builder().sender(this@parse).sandbox(true).namespace(namespaces).vars(map).build())
}


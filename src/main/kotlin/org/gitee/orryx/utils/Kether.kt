package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.ScriptManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.library.kether.Parser
import taboolib.library.kether.Parser.Action
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

internal fun Player.parse(action: String, map: Map<String, Any>): String {
    return adaptCommandSender(this).parse(action, map)
}

internal fun ProxyCommandSender.parse(action: String, map: Map<String, Any>): String {
    return KetherFunction.parse(action, ScriptOptions.builder().sender(this@parse).sandbox(true).namespace(namespaces).vars(map).build())
}

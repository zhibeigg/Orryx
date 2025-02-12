package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.ScriptManager
import org.joml.Matrix4d
import org.joml.Vector3d
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

const val ORRYX_NAMESPACE = "Orryx"

val orryxEnvironmentNamespaces = listOf("kether", ORRYX_NAMESPACE)

internal fun getBytes(actions: String): ByteArray {
    val s = if (actions.startsWith("def ")) actions else "def main = { $actions }"
    val texts = s.split("\n")
    return texts.mapNotNull { if (it.trim().startsWith("#")) null else it }.joinToString("\n").toByteArray(
        StandardCharsets.UTF_8
    )
}

internal fun ScriptFrame.bukkitPlayer(): Player {
    return script().sender?.castSafely<Player>() ?: error("Orryx脚本中Sender不是玩家")
}

internal fun ScriptContext.bukkitPlayer(): Player {
    return sender?.castSafely<Player>() ?: error("Orryx脚本中Sender不是玩家")
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

internal fun QuestReader.nextDest(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("dest"))
}

internal fun <T> ScriptFrame.destMatrix(dest: ParsedAction<*>?, func: (ScriptFrame.(dest: Matrix4d) -> T)): CompletableFuture<Any> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(Matrix4d()))
    } else {
        run(dest).str { key ->
            val matrix = Matrix4d()
            func(matrix)
            script()[key] = matrix
            matrix
        }
    }
}

internal fun <T> ScriptFrame.destVector(dest: ParsedAction<*>?, func: (ScriptFrame.(dest: AbstractVector) -> T)): CompletableFuture<Any> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(AbstractVector(Vector3d())))
    } else {
        run(dest).str { key ->
            val vector = AbstractVector(Vector3d())
            func(vector)
            script()[key] = vector
            vector
        }
    }
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

internal fun vector(): Parser<AbstractVector> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { vector ->
                when(vector) {
                    is AbstractVector -> vector
                    is Vector -> AbstractVector(Vector3d(vector.x, vector.y, vector.z))
                    is Vector3d -> AbstractVector(vector)
                    else -> AbstractVector(Vector3d())
                }
            }
        }
    }
}

fun <T> CompletableFuture<Any?>.vector(then: (AbstractVector) -> T): CompletableFuture<T> {
    return thenApply { vector ->
        then(
            when (vector) {
                is AbstractVector -> vector
                is Vector -> AbstractVector(Vector3d(vector.x, vector.y, vector.z))
                is Vector3d -> AbstractVector(vector)
                else -> AbstractVector(Vector3d())
            }
        )
    }.except { then(AbstractVector(Vector3d())) }
}

fun <T> CompletableFuture<Any?>.matrix(then: (Matrix4d) -> T): CompletableFuture<T> {
    return thenApply { matrix -> then(matrix as Matrix4d) }.except { then(Matrix4d()) }
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
    return KetherShell.eval(action, ScriptOptions.builder().sender(this@eval).sandbox(true).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun Player.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return adaptCommandSender(this).parse(actions, map)
}

internal fun Player.parse(action: String, map: Map<String, Any>): String {
    return adaptCommandSender(this).parse(action, map)
}

internal fun ProxyCommandSender.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return KetherFunction.parse(actions, ScriptOptions.builder().sender(this@parse).sandbox(true).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun ProxyCommandSender.parse(action: String, map: Map<String, Any>): String {
    return KetherFunction.parse(action, ScriptOptions.builder().sender(this@parse).sandbox(true).namespace(orryxEnvironmentNamespaces).vars(map).build())
}


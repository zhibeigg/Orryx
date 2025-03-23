package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.actions.effect.EffectBuilder
import org.gitee.orryx.core.kether.actions.effect.EffectSpawner
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Matrix3d
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

val orryxEnvironmentNamespaces = listOf(ORRYX_NAMESPACE, "kether")

val EMPTY_FUNCTION = {}

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

internal fun QuestReader.nextHeadActionOrNull(id: String): ParsedAction<*>? {
    return nextHeadActionOrNull(arrayOf(id))
}

internal fun QuestReader.nextHeadAction(id: String, def: Any): ParsedAction<*> {
    return nextHeadActionOrNull(arrayOf(id)) ?: literalAction(def)
}

internal fun QuestReader.nextTheyContainerOrNull(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("they"))
}

internal fun QuestReader.nextTheyContainerOrSelf(): ParsedAction<*> {
    return this.nextHeadAction("they", "@self")
}

internal fun QuestReader.nextDest(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("dest"))
}

internal fun <T> ScriptFrame.destMatrix(dest: ParsedAction<*>?, func: (ScriptFrame.(dest: Matrix3d) -> T)): CompletableFuture<T> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(Matrix3d()))
    } else {
        run(dest).matrix { matrix ->
            func(matrix)
        }
    }
}

internal fun <T> ScriptFrame.destVector(dest: ParsedAction<*>?, func: (ScriptFrame.(dest: IVector) -> T)): CompletableFuture<T> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(AbstractVector()))
    } else {
        run(dest).vector { vector ->
            func(vector)
        }
    }
}

internal fun <T> ScriptFrame.container(container: ParsedAction<*>?, def: IContainer, func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return if (container == null) {
        CompletableFuture.completedFuture(func(def))
    } else {
        run(container).thenApply {
            func(it.readContainer(script()).orElse(def))
        }
    }
}

internal fun <T> ScriptFrame.container(container: ParsedAction<*>, func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return run(container).thenApply {
        func(it.readContainer(script()).orElse(Container()))
    }
}

internal fun <T> ScriptFrame.containerOrSelf(container: ParsedAction<*>?, func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return container(container, self(), func)
}

internal fun vector(): Parser<AbstractVector> {
    return Parser.frame { r ->
        val action = r.nextParsedAction()
        Action {
            it.run(action).thenApply { vector ->
                when(vector) {
                    is AbstractVector -> vector
                    is Vector -> Vector3d(vector.x, vector.y, vector.z).abstract()
                    is Vector3d -> vector.abstract()
                    is ITargetLocation<*> -> AbstractVector(vector.location)
                    else -> AbstractVector()
                }
            }
        }
    }
}

fun <T> CompletableFuture<Any?>.vector(then: (IVector) -> T): CompletableFuture<T> {
    return thenApply { vector ->
        then(
            when (vector) {
                is AbstractVector -> vector
                is Vector -> Vector3d(vector.x, vector.y, vector.z).abstract()
                is Vector3d -> vector.abstract()
                is ITargetLocation<*> -> AbstractVector(vector.location)
                is String -> AbstractVector()
                else -> AbstractVector()
            }
        )
    }.except { then(AbstractVector()) }
}

fun <T> CompletableFuture<Any?>.effect(then: (EffectBuilder) -> T): CompletableFuture<T> {
    return thenApply { effect -> then(effect as? EffectBuilder ?: error("应传入粒子效果构建器但是传入了${effect?.javaClass?.name}")) }.except { then(EffectBuilder()) }
}

fun <T> CompletableFuture<Any?>.effectSpawner(then: (EffectSpawner) -> T): CompletableFuture<T> {
    return thenApply { effect -> then(effect as? EffectSpawner ?: error("应传入粒子生成器但是传入了${effect?.javaClass?.name}")) }
}

fun <T> CompletableFuture<Any?>.matrix(then: (Matrix3d) -> T): CompletableFuture<T> {
    return thenApply { matrix -> then(matrix as? Matrix3d ?: error("应传入矩阵但是传入了${matrix?.javaClass?.name}")) }.except { then(Matrix3d()) }
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
    return KetherShell.eval(action, ScriptOptions.builder().sender(this@eval).sandbox(false).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun Player.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return adaptCommandSender(this).parse(actions, map)
}

internal fun Player.parse(action: String, map: Map<String, Any>): String {
    return adaptCommandSender(this).parse(action, map)
}

internal fun ProxyCommandSender.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return KetherFunction.parse(actions, ScriptOptions.builder().sender(this@parse).sandbox(false).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun ProxyCommandSender.parse(action: String, map: Map<String, Any>): String {
    return KetherFunction.parse(action, ScriptOptions.builder().sender(this@parse).sandbox(false).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun ScriptContext.vector(key: String, def: IVector? = null): IVector? {
    return when(val vector = get<Any?>(key, def)) {
        is Vector -> vector.abstract()
        is taboolib.common.util.Vector -> vector.abstract()
        is Vector3d -> vector.abstract()
        is AbstractVector -> vector
        is ITargetLocation<*> -> AbstractVector(vector.location)
        else -> def
    }
}

fun ScriptFrame.skillCaster(func: Player.() -> CompletableFuture<Any?>): CompletableFuture<Any?> {
    return when (val parm = script().getParameterOrNull()) {
        is SkillParameter -> {
            parm.player.func()
        }

        is StationParameter<*> -> {
            parm.sender.castSafely<Player>()?.func() ?: error("Station发送者无Orryx信息")
        }

        else -> {
            script().bukkitPlayer().func()
        }
    }
}

enum class Method(vararg val symbols: String) {
    INCREASE("add", "+"), DECREASE("sub", "-"), MODIFY("set", "to", "="), NONE;
}
package org.gitee.orryx.utils

import com.mojang.datafixers.kinds.App
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.gitee.nodens.util.NODENS_NAMESPACE
import org.gitee.orryx.api.adapters.IVector
import org.gitee.orryx.api.adapters.vector.AbstractVector
import org.gitee.orryx.api.collider.local.ILocalCollider
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.ScriptManager.wikiActions
import org.gitee.orryx.core.kether.actions.effect.EffectBuilder
import org.gitee.orryx.core.kether.actions.effect.EffectSpawner
import org.gitee.orryx.core.kether.actions.math.hitbox.collider.local.None
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.targets.ITargetLocation
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.common.util.t
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.Parser
import taboolib.library.kether.Parser.*
import taboolib.library.kether.QuestAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import taboolib.module.kether.ParserHolder.command
import taboolib.module.kether.ParserHolder.option
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

const val ORRYX_NAMESPACE = "Orryx"

val orryxEnvironmentNamespaces = listOf(ORRYX_NAMESPACE, NODENS_NAMESPACE, "kether")

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
    return Container(linkedSetOf(bukkitPlayer().toTarget()))
}

internal fun ScriptFrame.world(): IContainer {
    return Container(bukkitPlayer().world.livingEntities.mapTo(linkedSetOf()) { it.toTarget() })
}

internal inline fun <T> ScriptFrame.keySetting(crossinline func: (setting: PlayerKeySetting) -> T): CompletableFuture<T> {
    return bukkitPlayer().keySetting {
        func(it)
    }
}

internal fun QuestReader.nextHeadActionOrNull(array: Array<out String>): ParsedAction<*>? {
    return try {
        mark()
        expects(*array)
        this.nextParsedAction()
    } catch (_: Exception) {
        reset()
        null
    }
}

internal fun QuestReader.nextHeadActionOrNull(id: String): ParsedAction<*>? {
    return nextHeadActionOrNull(arrayOf(id))
}

internal fun QuestReader.nextHeadAction(vararg id: String, def: Any): ParsedAction<*> {
    return nextHeadActionOrNull(id) ?: literalAction(def)
}

internal fun QuestReader.nextTheyContainerOrNull(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("they"))
}

internal fun QuestReader.nextTheyContainerOrSelf(): ParsedAction<*> {
    return this.nextHeadAction("they", def = "@self")
}

internal fun QuestReader.nextDest(): ParsedAction<*>? {
    return this.nextHeadActionOrNull(arrayOf("dest"))
}

internal inline fun <T> ScriptFrame.destQuaternion(dest: ParsedAction<*>?, crossinline func: (ScriptFrame.(dest: Quaterniond) -> T)): CompletableFuture<T> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(Quaterniond()))
    } else {
        run(dest).quaternion { quaternion ->
            func(quaternion)
        }
    }
}

internal inline fun <T> ScriptFrame.destMatrix(dest: ParsedAction<*>?, crossinline func: (ScriptFrame.(dest: Matrix3d) -> T)): CompletableFuture<T> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(Matrix3d()))
    } else {
        run(dest).matrix { matrix ->
            func(matrix)
        }
    }
}

internal inline fun <T> ScriptFrame.destVector(dest: ParsedAction<*>?, crossinline func: (ScriptFrame.(dest: IVector) -> T)): CompletableFuture<T> {
    return if (dest == null) {
        CompletableFuture.completedFuture(func(AbstractVector()))
    } else {
        run(dest).vector { vector ->
            func(vector)
        }
    }
}

internal inline fun <T> ScriptFrame.container(container: ParsedAction<*>?, def: IContainer, crossinline func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return if (container == null) {
        CompletableFuture.completedFuture(func(def))
    } else {
        run(container).thenCompose {
            it.readContainer(script())?.thenApply { container ->
                func(container)
            } ?: CompletableFuture.completedFuture(func(def))
        }
    }
}

internal inline fun <T> ScriptFrame.container(container: ParsedAction<*>, crossinline func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
    return run(container).thenCompose {
        it.readContainer(script())?.thenApply { container ->
            func(container)
        } ?: CompletableFuture.completedFuture(func(Container()))
    }
}

internal inline fun <T> ScriptFrame.containerOrSelf(container: ParsedAction<*>?, crossinline func: (ScriptFrame.(container: IContainer) -> T)): CompletableFuture<T> {
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
                    is String -> vector.parseVector()
                    else -> null
                }
            }
        }
    }
}

inline fun <T> CompletableFuture<Any?>.vector(crossinline then: (IVector) -> T): CompletableFuture<T> {
    return thenApply { vector ->
        then(
            when (vector) {
                is AbstractVector -> vector
                is Vector -> Vector3d(vector.x, vector.y, vector.z).abstract()
                is Vector3d -> vector.abstract()
                is ITargetLocation<*> -> AbstractVector(vector.location)
                is String -> vector.parseVector() ?: AbstractVector()
                else -> AbstractVector()
            }
        )
    }.except { then(AbstractVector()) }
}

inline fun <T> CompletableFuture<Any?>.effect(crossinline then: (EffectBuilder) -> T): CompletableFuture<T> {
    return thenApply { effect -> then(effect as? EffectBuilder ?: error("应传入粒子效果构建器但是传入了${effect?.javaClass?.name}")) }.except { then(EffectBuilder()) }
}

inline fun <T> CompletableFuture<Any?>.effectSpawner(crossinline then: (EffectSpawner) -> T): CompletableFuture<T> {
    return thenApply { effect -> then(effect as? EffectSpawner ?: error("应传入粒子生成器但是传入了${effect?.javaClass?.name}")) }
}

inline fun <T> CompletableFuture<Any?>.matrix(crossinline then: (Matrix3d) -> T): CompletableFuture<T> {
    return thenApply { matrix -> then(matrix as? Matrix3d ?: error("应传入矩阵但是传入了${matrix?.javaClass?.name}")) }.except { then(Matrix3d()) }
}

inline fun <T> CompletableFuture<Any?>.quaternion(crossinline then: (Quaterniond) -> T): CompletableFuture<T> {
    return thenApply { quaternion -> then(quaternion as? Quaterniond ?: error("应传入四元数但是传入了${quaternion?.javaClass?.name}")) }.except { then(Quaterniond()) }
}

inline fun <T> CompletableFuture<Any?>.collider(crossinline then: (ILocalCollider<*>) -> T): CompletableFuture<T> {
    return thenApply { collider -> then(collider as? ILocalCollider<*> ?: error("应传入碰撞箱但是传入了${collider?.javaClass?.name}")) }.except { then(None()) }
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
            it.run(action).thenCompose { container ->
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
    return KetherShell.eval(action, ScriptOptions.builder().sender(this@eval).sandbox(!debug).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun Player.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return adaptCommandSender(this).parse(actions, map)
}

internal fun Player.parse(action: String, map: Map<String, Any>): String {
    return adaptCommandSender(this).parse(action, map)
}

internal fun ProxyCommandSender.parse(actions: List<String>, map: Map<String, Any>): List<String> {
    return KetherFunction.parse(actions, ScriptOptions.builder().sender(this@parse).sandbox(!debug).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun ProxyCommandSender.parse(action: String, map: Map<String, Any>): String {
    return KetherFunction.parse(action, ScriptOptions.builder().sender(this@parse).sandbox(!debug).namespace(orryxEnvironmentNamespaces).vars(map).build())
}

internal fun ScriptContext.vector(key: String, def: IVector? = null): IVector? {
    return when(val vector = get<Any?>(key, def)) {
        is Vector -> vector.abstract()
        is taboolib.common.util.Vector -> vector.abstract()
        is Vector3d -> vector.abstract()
        is AbstractVector -> vector
        is ITargetLocation<*> -> AbstractVector(vector.location)
        is String -> vector.parseVector()
        else -> def
    }
}

fun ScriptFrame.skillCaster(func: Player.(parm: IParameter?) -> CompletableFuture<Any?>): CompletableFuture<Any?> {
    return when (val parm = script().getParameterOrNull()) {
        is SkillParameter -> {
            parm.player.func(parm)
        }

        is StationParameter<*> -> {
            parm.sender.castSafely<Player>()?.func(parm) ?: error("Station发送者无Orryx信息")
        }

        else -> {
            script().bukkitPlayer().func(parm)
        }
    }
}

/**
 * 确保[func]在主线程运行
 * */
inline fun <T> ensureSync(crossinline func: () -> T): CompletableFuture<T> {
    if (isPrimaryThread) {
        return CompletableFuture.completedFuture(func())
    } else {
        val future = CompletableFuture<T>()
        submit { future.complete(func()) }
        return future
    }
}

fun <T> scriptParser(vararg actions: org.gitee.orryx.module.wiki.Action, resolve: (QuestReader) -> QuestAction<T>): ScriptActionParser<T> {
    wikiActions += actions
    return ScriptActionParser(resolve)
}

fun <T> combinationParser(action: org.gitee.orryx.module.wiki.Action, builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
    wikiActions += action
    return combinationParser(builder)
}

inline fun <T> combinationParser(builder: ParserHolder.(Instance) -> App<Mu, Action<T>>): ScriptActionParser<T> {
    val parser = Parser.build(builder(ParserHolder, instance()))
    return ScriptActionParser { parser.resolve<T>(this) }
}

fun Throwable.printKetherErrorMessage(detailError: Boolean = false) {
    if (localizedMessage == null || detailError) {
        printStackTrace()
        return
    }
    if (this is IllegalStateException) {
        warning(message)
    }
    if (javaClass.name.endsWith("kether.LocalizedException") || javaClass.name.endsWith("kether.LocalizedException\$Concat")) {
        warning(
            """
                解析 Kether 语句时发生了意外的异常：
                Unexpected exception while parsing kether script:
            """.t()
        )
    } else {
        warning(
            """
                运行 Kether 语句时发生了意外的异常：
                Unexpected exception while running the kether script.
            """.t()
        )
    }
    localizedMessage.split('\n').forEach { warning(it) }
}
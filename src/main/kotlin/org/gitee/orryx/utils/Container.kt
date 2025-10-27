package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.skill.Icon
import org.gitee.orryx.core.targets.ITarget
import taboolib.common.platform.ProxyCommandSender
import taboolib.module.kether.ScriptContext
import taboolib.platform.util.onlinePlayers
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 移除容器中所有指定类型的目标
 * @return 原容器
 */
inline fun <reified T> IContainer.remove(): IContainer = apply {
    targets.removeIf { it is T }
}

/**
 * 获取容器中指定类型目标的不可变视图
 */
inline fun <reified T : ITarget<*>> IContainer.get(): List<T> {
    return targets.filterIsInstance<T>()
}

/**
 * 检查所有指定类型目标是否满足条件
 */
inline fun <reified T : ITarget<*>> IContainer.all(predicate: (T) -> Boolean): Boolean {
    return targets.all { it is T && predicate(it) }
}

/**
 * 检查所有指定类型目标是否都不满足条件
 */
inline fun <reified T : ITarget<*>> IContainer.none(predicate: (T) -> Boolean): Boolean {
    return targets.none { it is T && predicate(it) }
}

/**
 * 检查是否存在满足条件的指定类型目标
 */
inline fun <reified T : ITarget<*>> IContainer.any(predicate: (T) -> Boolean): Boolean {
    return targets.any { it is T && predicate(it) }
}

/**
 * 对指定类型目标执行操作
 */
inline fun <reified T : ITarget<*>> IContainer.forEachInstance(action: (T) -> Unit): IContainer = apply {
    targets.filterIsInstance<T>().forEach(action)
}

/**
 * 转换指定类型目标为[R]类型列表
 */
inline fun <reified T : ITarget<*>, R> IContainer.mapInstance(transform: (T) -> R): List<R> {
    return targets.mapNotNull { (it as? T)?.let(transform) }
}

/**
 * 转换指定类型目标为非空[R]类型列表
 */
inline fun <reified T : ITarget<*>, R : Any> IContainer.mapNotNullInstance(transform: (T) -> R?): List<R> {
    return targets.mapNotNull { (it as? T)?.let(transform) }
}

/**
 * 获取第一个指定类型目标（非空）
 */
inline fun <reified T : ITarget<*>> IContainer.firstInstance(): T {
    return firstInstanceOrNull() ?: throw NoSuchElementException()
}

/**
 * 获取第一个指定类型目标（可为空）
 */
inline fun <reified T : ITarget<*>> IContainer.firstInstanceOrNull(): T? {
    return targets.firstOrNull { it is T } as T?
}

/**
 * 合并多个容器
 */
fun mergeAll(containers: List<IContainer>): IContainer {
    return containers.fold(Container()) { acc, container ->
        acc.apply { merge(container) }
    }
}

fun mergeAll(containers: List<CompletableFuture<IContainer>>): CompletableFuture<IContainer> {
    return CompletableFuture.allOf(*containers.toTypedArray()).thenApply {
        containers.fold(Container()) { acc, container ->
            acc.apply { merge(container.getNow(Container())) }
        }
    }
}

/**
 * 安全容器解析（性能优化版）
 */
internal fun Any?.readContainer(context: ScriptContext): CompletableFuture<IContainer>? {
    return when {
        // Adyeshach 实体处理
        AdyeshachPlugin.isEnabled && this is EntityInstance -> {
            debug("readEntityInstance")
            toTarget().readContainer(context)
        }

        // 基础类型处理
        this == null -> null.also { debug("readNull") }
        this is String -> StringParser(this).container(context).also { debug("readString") }
        this is IContainer -> CompletableFuture.completedFuture(this).also { debug("readIContainer") }
        this is ITarget<*> -> {
            debug("readTarget")
            CompletableFuture.completedFuture(Container(linkedSetOf(this)))
        }

        // 实体相关处理
        this is Player -> toTarget().readContainer(context).also { debug("readPlayer") }
        this is Entity -> toTarget().readContainer(context).also { debug("readEntity") }
        this is Location -> toTarget().readContainer(context).also { debug("readLocation") }
        this is UUID -> {
            debug("readUUID")
            (Bukkit.getEntity(this)?.readContainer(context) ?: CompletableFuture.completedFuture(Container()))
        }

        // 集合处理
        this is Iterable<*> -> mergeAll(mapNotNull { it?.readContainer(context) }).also { debug("readIterable") }

        // 命令发送者处理
        this is ProxyCommandSender -> castSafely<Player>()?.toTarget()?.readContainer(context).also { debug("readProxyCommandSender") }

        else -> null
    }
}

/**
 * 安全获取容器或返回默认值
 */
internal fun IContainer?.orElse(default: IContainer): IContainer = this ?: default

/**
 * 创建世界玩家容器
 */
fun worldPlayerWorldContainer(world: World): IContainer {
    return Container(world.players.mapTo(LinkedHashSet(), Player::toTarget))
}

/**
 * 创建服务器玩家容器
 */
val serverPlayerContainer: IContainer
    get() = Container(onlinePlayers.mapTo(LinkedHashSet(), Player::toTarget))
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
import org.gitee.orryx.core.targets.ITarget
import taboolib.common.platform.ProxyCommandSender
import taboolib.module.kether.ScriptContext
import java.util.*

/**
 * 移除容器中的类型目标
 * @param T [ITarget]类型
 * @return 原容器
 * */
inline fun <reified T> IContainer.remove(): IContainer {
    targets.removeIf {
        it is T
    }
    return this
}

/**
 * 获取容器中的类型目标
 * @param T [ITarget]类型
 * @return 满足的目标
 * */
inline fun <reified T : ITarget<*>> IContainer.get(): MutableSet<T> {
    return targets.filterIsInstance<T>().toMutableSet()
}

/**
 * 类型中全部满足true时返回true
 * @param T [ITarget]类型
 * @param func 对目标执行的匿名方法
 * @return 是否全部满足
 * */
inline fun <reified T : ITarget<*>> IContainer.all(func: (target: T) -> Boolean): Boolean {
    return get<T>().all { func(it) }
}

/**
 * 类型中任一满足true时返回true
 * @param T [ITarget]类型
 * @param func 对目标执行的匿名方法
 * @return 是否任一满足
 * */
inline fun <reified T : ITarget<*>> IContainer.any(func: (target: T) -> Boolean): Boolean {
    return get<T>().any { func(it) }
}

/**
 * 对某类型循环执行方法
 * @param T [ITarget]类型
 * @param func 对目标执行的匿名方法
 * @return 原容器
 * */
inline fun <reified T: ITarget<*>> IContainer.forEachInstance(func: (target: T) -> Unit): IContainer {
    targets.forEach {
        if (it is T) {
            func(it)
        }
    }
    return this
}

/**
 * 对某类型循环执行方法获得[R]并返回列表
 * @param T [ITarget]类型
 * @param func 对目标执行的匿名方法
 * @return [R]列表
 * */
inline fun <reified T: ITarget<*>, R : Any> IContainer.mapInstance(func: (target: T) -> R?): List<R?> {
    return targets.mapNotNull {
        if (it is T) {
            func(it)
        } else {
            null
        }
    }
}

/**
 * 对某类型循环执行方法获得[R]并返回无NULL列表
 * @param T [ITarget]类型
 * @param func 对目标执行的匿名方法
 * @return [R]列表
 * */
inline fun <reified T: ITarget<*>, R : Any> IContainer.mapNotNullInstance(func: (target: T) -> R?): List<R> {
    return mapInstance<T, R> { func(it) }.mapNotNullTo(ArrayList()) { it }
}

/**
 * 获取第一个
 * @throws NoSuchElementException 如果容器为空
 * */
inline fun <reified T: ITarget<*>> IContainer.firstInstance(): T {
    return firstInstanceOrNull<T>() ?: throw NoSuchElementException()
}

/**
 * 获取第一个或null
 * */
inline fun <reified T: ITarget<*>> IContainer.firstInstanceOrNull(): T? {
    var target: T? = null
    targets.forEach {
        if (it is T) {
            target = it
        }
    }
    return target
}

fun mergeAll(containers: List<IContainer>): IContainer {
    return Container().apply {
        containers.forEach {
            merge(it)
        }
    }
}

internal fun Any?.readContainer(context: ScriptContext): IContainer? {
    if (AdyeshachPlugin.isEnabled && this is EntityInstance) {
        debug("readEntityInstance")
        return toTarget().readContainer(context)
    }
    return when (this) {
        is String -> {
            debug("readString")
            StringParser(this).container(context)
        }
        is Player -> {
            debug("readPlayer")
            toTarget().readContainer(context)
        }
        is IContainer -> {
            debug("readIContainer")
            this
        }
        is Entity -> {
            debug("readEntity")
            toTarget().readContainer(context)
        }
        is Location -> {
            debug("readLocation")
            toTarget().readContainer(context)
        }
        is UUID -> {
            debug("readUUID")
            Bukkit.getEntity(this)?.readContainer(context) ?: Container()
        }
        is ITarget<*> -> {
            debug("readTarget")
            Container(mutableSetOf(this))
        }
        is Iterable<*> -> {
            debug("readIterable")
            mergeAll(mapNotNull { readContainer(context) })
        }
        is ProxyCommandSender -> {
            debug("readProxyCommandSender")
            castSafely<Player>()?.toTarget().readContainer(context)
        }
        null -> {
            debug("readNull")
            null
        }
        else -> {
            debug("readElse")
            Container()
        }
    }
}

internal fun IContainer?.orElse(container: IContainer): IContainer {
    return this ?: container
}

fun worldPlayerWorldContainer(world: World) = Container(world.players.map { it.toTarget() }.toMutableSet())
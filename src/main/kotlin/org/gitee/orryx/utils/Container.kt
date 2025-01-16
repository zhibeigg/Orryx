package org.gitee.orryx.utils

import ink.ptms.adyeshach.core.entity.EntityInstance
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.common.platform.ProxyCommandSender
import taboolib.module.kether.ScriptContext
import java.util.*

internal inline fun <reified T> IContainer.remove(): IContainer {
    targets.removeIf {
        it is T
    }
    return this
}

internal inline fun <reified T : ITarget<*>> IContainer.get(): MutableSet<T> {
    return targets.filterIsInstance<T>().toMutableSet()
}

internal inline fun <reified T: ITarget<*>> IContainer.forEachInstance(func: (target: T) -> Unit): IContainer {
    targets.forEach {
        if (it is T) {
            func(it)
        }
    }
    return this
}

internal fun mergeAll(containers: List<IContainer>): IContainer {
    return Container().apply {
        containers.forEach {
            merge(it)
        }
    }
}

internal fun Any?.readContainer(context: ScriptContext): IContainer? {
    if (AdyeshachEnabled && this is EntityInstance) {
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

package org.gitee.orryx.core.parser

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.selector.SelectorInit
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.ensureSync
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.info
import taboolib.common.util.Vector
import taboolib.common.util.unsafeLazy
import taboolib.library.xseries.XParticle
import taboolib.module.kether.ScriptContext
import java.util.concurrent.CompletableFuture

class StringParser(val value: String) {

    class Entry(val reverse: Boolean, val head: String, val body: MutableList<String>)

    //"@fuck 123 123 @pvp 1 !@team"
    val entries: getValue by unsafeLazy {
        var i = -1
        val entry = mutableListOf<Entry>()
        value.trim().split(" ").forEach {
            when {
                it.startsWith("@") -> {
                    i++
                    entry += Entry(false, it.removePrefix("@"), mutableListOf())
                }
                it.startsWith("!@") -> {
                    i++
                    entry += Entry(true, it.removePrefix("!@"), mutableListOf())
                }
                else -> {
                    if (i == -1) return@forEach
                    entry[i].body.add(it)
                }
            }
        }
        entry
    }

    fun syncContainer(context: ScriptContext): IContainer {
        val container = Container()
        entries.forEach { entry ->
            when(val selector = SelectorInit.getSelector(entry.head.uppercase())) {
                is ISelectorStream -> {
                    selector.processStream(container, context, entry)
                }
                is ISelectorGeometry -> {
                    container.targets.addAll(selector.getTargets(context, entry))
                }
                null -> {
                    info("选择器${entry.head}未注册")
                }
            }
        }
        return container
    }

    fun container(context: ScriptContext): CompletableFuture<IContainer> {
        return ensureSync {
            val container = Container()
            entries.forEach { entry ->
                when(val selector = SelectorInit.getSelector(entry.head.uppercase())) {
                    is ISelectorStream -> {
                        selector.processStream(container, context, entry)
                    }
                    is ISelectorGeometry -> {
                        container.targets.addAll(selector.getTargets(context, entry))
                    }
                    null -> {
                        info("选择器${entry.head}未注册")
                    }
                }
            }
            container
        }
    }

    fun stream(container: IContainer, context: ScriptContext): IContainer {
        entries.forEach { entry ->
            when(val selector = SelectorInit.getSelector(entry.head.uppercase())) {
                is ISelectorStream -> {
                    selector.processStream(container, context, entry)
                }
                is ISelectorGeometry -> {
                    container.targets.addAll(selector.getTargets(context, entry))
                }
                null -> {
                    info("选择器${entry.head}未注册")
                }
            }
        }
        return container
    }

    fun showAFrame(context: ScriptContext): IContainer {
        val container = Container()
        entries.forEach { entry ->
            when(val selector = SelectorInit.getSelector(entry.head.uppercase())) {
                is ISelectorGeometry -> {
                    selector.aFrameShowLocations(context, entry).forEach {
                        adaptPlayer(context.bukkitPlayer()).sendParticle(XParticle.DUST.name, it, Vector(), 1, 0.0, null)
                    }
                }
                null -> {
                    info("选择器${entry.head}未注册")
                }
            }
        }
        return container
    }

}
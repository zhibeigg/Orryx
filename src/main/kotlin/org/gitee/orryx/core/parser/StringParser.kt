package org.gitee.orryx.core.parser

import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.selector.ISelectorFilter
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.selector.SelectorInit
import org.gitee.orryx.utils.removeIf
import taboolib.common.platform.function.info
import taboolib.module.kether.ScriptContext

class StringParser(val value: String) {

    class Entry(val reverse: Boolean, val head: String, val body: MutableList<String>)

    //"@fuck 123 123 @pvp 1 !@team"
    val entries by lazy {
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
                    entry[i].body.add(it)
                }
            }
        }
        entry
    }

    fun container(context: ScriptContext): IContainer {
        val container = Container()
        entries.forEach { entry ->
            when(val selector = SelectorInit.getSelector(entry.head.uppercase())) {
                is ISelectorStream -> {
                    selector.joinContainer(container, context, entry)
                }
                is ISelectorFilter -> {
                    container.targets.removeIf(selector, context, entry)
                }
                is ISelectorGeometry -> {
                    container.targets += selector.getTargets(context, entry)
                }
                null -> {
                    info("选择器${entry.head}未注册")
                }
            }
        }
        return container
    }

}
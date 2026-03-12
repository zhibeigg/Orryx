package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import taboolib.module.kether.ScriptContext

object Unique: ISelectorStream {

    override val keys = arrayOf("unique", "distinct")

    override val wiki: Selector
        get() = Selector.new("去重", keys, SelectorType.STREAM)
            .addExample("@unique")
            .description("移除容器中重复的目标（按UUID或坐标去重）")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val seen = mutableSetOf<Any>()
        container.removeIf { target ->
            val key = when (target) {
                is ITargetEntity<*> -> target.entity.uniqueId
                is ITargetLocation<*> -> {
                    val loc = target.location
                    Triple(loc.world?.name, Triple(loc.x, loc.y, loc.z), loc.world)
                }
                else -> target
            }
            !seen.add(key)
        }
    }
}

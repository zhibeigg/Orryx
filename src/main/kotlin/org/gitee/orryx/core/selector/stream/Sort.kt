package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Sort: ISelectorStream {

    override val keys = arrayOf("sort")

    override val wiki: Selector
        get() = Selector.new("排序", keys, SelectorType.STREAM)
            .addExample("@sort near")
            .addExample("@sort health")
            .addParm(Type.STRING, "排序模式: near=按距离从近到远, far=从远到近, random=随机打乱, health=按血量从低到高", "near")
            .description("对容器中的目标进行排序")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val mode = parameter.read<String>(0, "near").lowercase()
        val origin = context.getParameter().origin ?: return

        val originLoc = origin.location

        val sorted = when (mode) {
            "near" -> container.targets.sortedBy {
                when (it) {
                    is ITargetLocation<*> -> it.location.distanceSquared(originLoc)
                    else -> Double.MAX_VALUE
                }
            }
            "far" -> container.targets.sortedByDescending {
                when (it) {
                    is ITargetLocation<*> -> it.location.distanceSquared(originLoc)
                    else -> 0.0
                }
            }
            "random" -> container.targets.shuffled()
            "health" -> container.targets.sortedBy {
                if (it is ITargetEntity<*>) {
                    val source = it.getSource()
                    if (source is org.bukkit.entity.LivingEntity) {
                        source.health
                    } else Double.MAX_VALUE
                } else Double.MAX_VALUE
            }
            else -> return
        }

        container.targets.clear()
        container.targets.addAll(sorted)
    }
}

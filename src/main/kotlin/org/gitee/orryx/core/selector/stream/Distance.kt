package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Distance: ISelectorStream {

    override val keys = arrayOf("distance", "dist")

    override val wiki: Selector
        get() = Selector.new("距离过滤", keys, SelectorType.STREAM)
            .addExample("@distance 5 10")
            .addParm(Type.DOUBLE, "最小距离", "0.0")
            .addParm(Type.DOUBLE, "最大距离", "32.0")
            .description("按距离原点的范围过滤目标，只保留距离在指定范围内的目标")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val minDist = parameter.read<Double>(0, 0.0)
        val maxDist = parameter.read<Double>(1, 32.0)
        val origin = context.getParameter().origin ?: return

        val originLoc = origin.location
        val minDistSq = minDist * minDist
        val maxDistSq = maxDist * maxDist

        if (parameter.reverse) {
            // !@distance: 移除在范围内的，保留范围外的
            container.removeIf {
                if (it is ITargetLocation<*>) {
                    val distSq = it.location.distanceSquared(originLoc)
                    distSq in minDistSq..maxDistSq
                } else {
                    false
                }
            }
        } else {
            // @distance: 只保留在范围内的
            container.removeIf {
                if (it is ITargetLocation<*>) {
                    val distSq = it.location.distanceSquared(originLoc)
                    distSq !in minDistSq..maxDistSq
                } else {
                    true
                }
            }
        }
    }
}

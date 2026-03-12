package org.gitee.orryx.core.selector.stream

import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.utils.getParameter
import taboolib.module.kether.ScriptContext

object Sight: ISelectorStream {

    override val keys = arrayOf("sight", "visible")

    override val wiki: Selector
        get() = Selector.new("视线可见过滤", keys, SelectorType.STREAM)
            .addExample("@sight")
            .addExample("!@sight")
            .description("过滤视线可达的实体，@sight只保留可见实体，!@sight只保留被遮挡实体")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val origin = context.getParameter().origin ?: return
        val originSource = origin.getSource()

        // 只有原点是LivingEntity时才能使用hasLineOfSight
        val originEntity = when (originSource) {
            is LivingEntity -> originSource
            else -> return
        }

        if (parameter.reverse) {
            // !@sight: 只保留被遮挡的实体
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    val source = it.getSource()
                    if (source is org.bukkit.entity.Entity) {
                        originEntity.hasLineOfSight(source)
                    } else false
                } else false
            }
        } else {
            // @sight: 只保留视线可达的实体
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    val source = it.getSource()
                    if (source is org.bukkit.entity.Entity) {
                        !originEntity.hasLineOfSight(source)
                    } else true
                } else false
            }
        }
    }
}

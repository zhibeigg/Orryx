package org.gitee.orryx.core.selector.stream

import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Health: ISelectorStream {

    override val keys = arrayOf("health", "hp")

    override val wiki: Selector
        get() = Selector.new("血量过滤", keys, SelectorType.STREAM)
            .addExample("@health 0 0.5")
            .addParm(Type.DOUBLE, "最小血量百分比", "0.0")
            .addParm(Type.DOUBLE, "最大血量百分比", "1.0")
            .description("按血量百分比过滤实体，适合斩杀类技能")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val minPercent = parameter.read<Double>(0, 0.0)
        val maxPercent = parameter.read<Double>(1, 1.0)

        if (parameter.reverse) {
            // !@health: 移除血量在范围内的
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    val source = it.getSource()
                    if (source is LivingEntity) {
                        val maxHealth = source.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return@removeIf false
                        val percent = source.health / maxHealth
                        percent in minPercent..maxPercent
                    } else false
                } else false
            }
        } else {
            // @health: 只保留血量在范围内的
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    val source = it.getSource()
                    if (source is LivingEntity) {
                        val maxHealth = source.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: return@removeIf true
                        val percent = source.health / maxHealth
                        percent !in minPercent..maxPercent
                    } else true
                } else true
            }
        }
    }
}

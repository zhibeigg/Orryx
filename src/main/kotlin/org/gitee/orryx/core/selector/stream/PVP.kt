package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import taboolib.module.kether.ScriptContext

object PVP: ISelectorStream {

    override val keys = arrayOf("pvp")

    override val wiki: Selector
        get() = Selector.new("pvp筛选", keys, SelectorType.STREAM)
            .addExample("@pvp")
            .addExample("!@pvp")
            .description("筛选是否为pvp")

    override fun processStream(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        if (parameter.reverse) {
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    it.entity.world.pvp
                } else {
                    false
                }
            }
        } else {
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    !it.entity.world.pvp
                } else {
                    false
                }
            }
        }
    }
}
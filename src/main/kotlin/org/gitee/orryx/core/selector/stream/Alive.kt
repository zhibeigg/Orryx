package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import taboolib.module.kether.ScriptContext

object Alive: ISelectorStream {

    override val keys = arrayOf("alive")

    override val wiki: Selector
        get() = Selector.new("存活过滤", keys, SelectorType.STREAM)
            .addExample("@alive")
            .addExample("!@alive")
            .description("过滤存活/死亡实体，@alive只保留存活实体，!@alive只保留死亡实体")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        if (parameter.reverse) {
            // !@alive: 只保留死亡/无效实体
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    it.entity.isValid && !it.entity.isDead
                } else {
                    false
                }
            }
        } else {
            // @alive: 只保留存活实体
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    !it.entity.isValid || it.entity.isDead
                } else {
                    false
                }
            }
        }
    }
}

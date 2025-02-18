package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

/**
 * Sender转化为玩家
 * ```
 * @self
 * !@self
 * ```
 * */
object Self: ISelectorStream {

    override val keys: Array<String>
        get() = arrayOf("self")

    override fun joinContainer(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        if (parameter.reverse) {
            container.removeIf {
                if (it is ITargetEntity<*>) {
                    it.entity.uniqueId == context.bukkitPlayer().uniqueId
                } else {
                    false
                }
            }
        } else {
            container.add(context.bukkitPlayer().toTarget())
        }
    }

}
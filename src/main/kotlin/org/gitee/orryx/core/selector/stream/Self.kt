package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

/**
 * Sender转化为玩家
 * ```
 * @self
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
            container.remove(context.bukkitPlayer().toTarget())
        } else {
            container.add(context.bukkitPlayer().toTarget())
        }
    }

}
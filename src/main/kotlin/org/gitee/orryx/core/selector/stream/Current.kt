package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

/**
 * Sender的当前位置
 * ```
 * @current e
 * @current l
 * ```
 * */
object Current: ISelectorStream {

    override val keys: Array<String>
        get() = arrayOf("current")

    override fun joinContainer(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        val mode = parameter.read(0, "l")
        when(mode) {
            "l" -> container.add(context.bukkitPlayer().location.toTarget())
            "e" -> container.add(context.bukkitPlayer().eyeLocation.toTarget())
        }
    }

}
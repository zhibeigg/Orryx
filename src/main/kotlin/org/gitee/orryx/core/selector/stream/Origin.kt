package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

object Origin: ISelectorStream {

    override val keys = arrayOf("origin")

    override val wiki: Selector
        get() = Selector.new("原点位置", keys, SelectorType.STREAM)
            .addExample("@origin e")
            .addExample("@origin l")
            .addParm(Type.SYMBOL, "位置模式，l代表脚底，e代表眼睛", "l")
            .description("原点位置")

    override fun processStream(
        container: IContainer,
        context: ScriptContext,
        parameter: StringParser.Entry
    ) {
        val mode = parameter.read(0, "l")
        when(mode) {
            "l" -> container.add(context.getParameter().origin!!.location.toTarget())
            "e" -> container.add(context.getParameter().origin!!.eyeLocation.toTarget())
        }
    }
}
package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Random: ISelectorStream {

    override val keys = arrayOf("random", "rand")

    override val wiki: Selector
        get() = Selector.new("随机抽取", keys, SelectorType.STREAM)
            .addExample("@random 3")
            .addParm(Type.INT, "数量", "1")
            .description("从容器中随机抽取N个目标")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val amount = parameter.read<Int>(0, 1)

        if (container.targets.size <= amount) return

        val retained = container.targets.shuffled().take(amount)
        container.targets.clear()
        container.targets.addAll(retained)
    }
}

package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import taboolib.common.platform.function.warning
import taboolib.module.kether.ScriptContext

object Amount: ISelectorStream {

    override val keys = arrayOf("amount")

    override val wiki: Selector
        get() = Selector.new("数量过滤", keys, SelectorType.STREAM)
            .addExample("@amount 1 drop")
            .addExample("@amount 1 take")
            .addParm(Type.SYMBOL, "drop丢弃前方take丢弃后方", "take")
            .description("丢弃超出指定容器大小范围的目标，可选择丢弃方式")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val amount = parameter.read<Int>(0, 1)
        val type = parameter.read<String>(1, "take").lowercase()
        if (container.targets.size > amount) {
            when (type) {
                "drop" -> {
                    val toDrop = container.targets.size - amount
                    val retained = container.drop(toDrop)
                    container.targets.clear()
                    container.targets.addAll(retained)
                }
                "take" -> {
                    val retained = container.take(amount)
                    container.targets.clear()
                    container.targets.addAll(retained)
                }
                else -> warning("@amount选择器出现未知遗弃类型 发生在${context.getParameter()}")
            }
        }
    }
}
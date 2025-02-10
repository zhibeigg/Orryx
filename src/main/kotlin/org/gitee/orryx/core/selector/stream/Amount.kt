package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import taboolib.common.platform.function.warning
import taboolib.module.kether.ScriptContext

/**
 * drop丢弃前方take丢弃后方
 * ```
 * @amount 1 drop
 * @amount 1 take
 * ```
 * */
object Amount: ISelectorStream {

    override val keys: Array<String>
        get() = arrayOf("amount")

    override fun joinContainer(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val amount = parameter.read<Int>(0, "1")
        val type = parameter.read<String>(1, "take")
        if (container.targets.size > amount) {
            if (type.lowercase() == "drop") {
                val list = container.drop(container.targets.size - amount)
                container.targets.removeIf { it !in list }
            } else if(type.lowercase() == "take") {
                val list = container.take(amount)
                container.targets.removeIf { it !in list }
            } else {
                warning("@amount选择器出现未知遗弃类型 发生在${context.getParameter()}")
            }
        }
    }

}
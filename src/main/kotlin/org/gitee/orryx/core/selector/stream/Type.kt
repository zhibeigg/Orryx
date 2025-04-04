package org.gitee.orryx.core.selector.stream

import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Type: ISelectorStream {

    override val keys: Array<String> = arrayOf("type")

    override val wiki: Selector
        get() = Selector.new("实体/目标类型过滤", keys, SelectorType.STREAM)
            .addExample("@type player,pig")
            .addExample("!@type location")
            .addParm(Type.STRING, "指定实体类型，location表示坐标类型目标，用英文逗号分割", "player")
            .description("实体/目标类型过滤")

    private fun locationCheck(types: List<String>): Boolean {
        return "LOCATION" in types || "LOC" in types
    }

    override fun joinContainer(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val types = parameter.read<String>(0, "player").uppercase().split(",")

        if (parameter.reverse) {
            container.removeIf {
                when (it) {
                    is ITargetEntity<*> -> it.entity.type in types
                    is ITargetLocation<*> -> locationCheck(types)
                    else -> false
                }
            }
        } else {
            container.removeIf {
                when (it) {
                    is ITargetEntity<*> -> it.entity.type !in types
                    is ITargetLocation<*> -> !locationCheck(types)
                    else -> true
                }
            }
        }
    }

}
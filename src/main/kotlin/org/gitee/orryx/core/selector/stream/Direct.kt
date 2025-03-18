package org.gitee.orryx.core.selector.stream

import org.bukkit.Location
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Selector
import org.gitee.orryx.core.wiki.SelectorType
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.bukkit
import org.gitee.orryx.utils.mapNotNullInstance
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.vector
import taboolib.module.kether.ScriptContext

object Direct: ISelectorStream {

    override val keys = arrayOf("amount")

    override val wiki: Selector
        get() = Selector.new("改变视角向量", Server.keys, SelectorType.STREAM)
            .addExample("@vector Vector")
            .addExample("!@vector Vector")
            .addParm(Type.STRING, "向量的存储Key", "a")
            .description("将所有目标的视角向量修改")

    override fun joinContainer(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val vector = parameter.read<String>(0, "a")
        val vectorBukkit = if (parameter.reverse) {
            context.vector(vector)?.negate()?.bukkit()
        } else {
            context.vector(vector)?.bukkit()
        } ?: return
        val list = container.mapNotNullInstance<ITargetLocation<*>, ITargetLocation<Location>> {
            LocationTarget(it.location.clone().apply { direction = vectorBukkit })
        }
        container.removeIf { it is ITargetLocation }.addAll(list)
    }

}
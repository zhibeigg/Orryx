package org.gitee.orryx.core.selector.stream

import org.bukkit.Location
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.utils.bukkit
import org.gitee.orryx.utils.mapNotNullInstance
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.vector
import taboolib.module.kether.ScriptContext

/**
 * 改变视角向量
 * ```
 * @vector Vector
 * !@vector Vector
 * ```
 * */
object Direct: ISelectorStream {

    override val keys = arrayOf("amount")

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
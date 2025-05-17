package org.gitee.orryx.core.selector.stream

import org.bukkit.Location
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.bukkit
import org.gitee.orryx.utils.mapNotNullInstance
import org.gitee.orryx.utils.read
import org.joml.Vector3d
import taboolib.module.kether.ScriptContext

object Direct: ISelectorStream {

    override val keys = arrayOf("direct")

    override val wiki: Selector
        get() = Selector.new("改变视角向量", keys, SelectorType.STREAM)
            .addExample("@direct x y z")
            .addExample("!@direct x y z")
            .addParm(Type.DOUBLE, "x", "0.0")
            .addParm(Type.DOUBLE, "y", "0.0")
            .addParm(Type.DOUBLE, "z", "0.0")
            .description("将所有目标的视角向量修改")

    override fun processStream(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)

        val vectorBukkit = if (parameter.reverse) {
            Vector3d(x, y, z).negate().bukkit()
        } else {
            Vector3d(x, y, z).bukkit()
        }
        val list = container.mapNotNullInstance<ITargetLocation<*>, ITargetLocation<Location>> {
            LocationTarget(it.location.clone().apply { direction = vectorBukkit })
        }
        container.removeIf { it is ITargetLocation }.addAll(list)
    }
}
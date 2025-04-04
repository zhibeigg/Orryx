package org.gitee.orryx.core.selector.stream

import org.bukkit.Location
import org.bukkit.util.Vector
import org.gitee.orryx.core.container.IContainer
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorStream
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.direction
import org.gitee.orryx.utils.mapNotNullInstance
import org.gitee.orryx.utils.read
import taboolib.module.kether.ScriptContext

object Offset: ISelectorStream {

    override val keys = arrayOf("offset")

    override val wiki: Selector
        get() = Selector.new("改变坐标", keys, SelectorType.STREAM)
            .addExample("@offset 2 0 0 true false")
            .addExample("!@offset x y z yaw pitch")
            .addParm(Type.DOUBLE, "x", "0.0")
            .addParm(Type.DOUBLE, "y", "0.0")
            .addParm(Type.DOUBLE, "z", "0.0")
            .addParm(Type.BOOLEAN, "是否随YAW改变", "false")
            .addParm(Type.BOOLEAN, "是否随PITCH改变", "false")
            .description("将所有目标转换成location并位移")

    override fun joinContainer(container: IContainer, context: ScriptContext, parameter: StringParser.Entry) {
        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Boolean>(3, false)
        val pitch = parameter.read<Boolean>(4, false)

        val list = if (yaw) {
            container.mapNotNullInstance<ITargetLocation<*>, ITargetLocation<Location>> {
                LocationTarget(it.location.add(it.direction(x, y, z, pitch)))
            }
        } else {
            val vector = Vector(x, y, z)
            container.mapNotNullInstance<ITargetLocation<*>, ITargetLocation<Location>> {
                LocationTarget(it.location.add(vector))
            }
        }
        container.removeIf { it is ITargetLocation }.addAll(list)
    }

}
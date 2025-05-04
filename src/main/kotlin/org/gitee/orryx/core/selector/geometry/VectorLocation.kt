package org.gitee.orryx.core.selector.geometry

import org.bukkit.util.Vector
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.direction
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.module.kether.ScriptContext

object VectorLocation: ISelectorGeometry {

    override val keys = arrayOf("vector", "direction")

    override val wiki: Selector
        get() = Selector.new("原点偏移", keys, SelectorType.GEOMETRY)
            .addExample("@vector 2 3 5 true false")
            .addExample("@direction x y z yaw pitch")
            .addParm(Type.DOUBLE, "前方", "0.0")
            .addParm(Type.DOUBLE, "上方", "0.0")
            .addParm(Type.DOUBLE, "右方", "0.0")
            .addParm(Type.BOOLEAN, "是否随YAW改变", "false")
            .addParm(Type.BOOLEAN, "是否随PITCH改变", "false")
            .description("根据原点实体朝向来确认点的位置")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Boolean>(3, false)
        val pitch = parameter.read<Boolean>(4, false)

        val direction = if (yaw) {
            origin.direction(x, y, z, pitch)
        } else {
            Vector(x, y, z)
        }
        val loc = origin.eyeLocation.clone().add(direction)
        return listOf(loc.toTarget())
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Boolean>(3, false)
        val pitch = parameter.read<Boolean>(4, false)

        val direction = if (yaw) {
            origin.direction(x, y, z, pitch)
        } else {
            Vector(x, y, z)
        }
        val loc = origin.eyeLocation.clone().add(direction)
        return listOf(adaptLocation(loc))
    }
}
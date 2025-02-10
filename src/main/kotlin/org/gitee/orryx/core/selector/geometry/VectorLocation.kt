package org.gitee.orryx.core.selector.geometry

import org.bukkit.Location
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.direction
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext


/**
 * 根据原点实体朝向来确认点的位置
 * ```
 * @vector 2 3 5
 * @direction 前 上 右
 * ```
 */
object VectorLocation: ISelectorGeometry {

    override val keys: Array<String>
        get() = arrayOf("vector", "direction")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, "0.0")
        val y = parameter.read<Double>(1, "0.0")
        val z = parameter.read<Double>(2, "0.0")

        val direction = origin.direction(x, y, z)
        val loc = origin.eyeLocation.clone().add(direction)
        return listOf(loc.toTarget())
    }

    override fun showAFrame(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, "0.0")
        val y = parameter.read<Double>(1, "0.0")
        val z = parameter.read<Double>(2, "0.0")

        val direction = origin.direction(x, y, z)
        val loc = origin.eyeLocation.clone().add(direction)

        return listOf(loc)
    }

}
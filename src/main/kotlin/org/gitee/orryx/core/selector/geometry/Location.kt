package org.gitee.orryx.core.selector.geometry

import org.bukkit.Bukkit
import org.bukkit.Location
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

/**
 * 具体坐标点
 * ```
 * @location 2 3 5 1 2
 * @location x y z yaw pitch world
 * ```
 */
object Location: ISelectorGeometry {

    override val keys: Array<String>
        get() = arrayOf("location")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Float>(3, 0.0f)
        val pitch = parameter.read<Float>(4, 0.0f)
        val world = parameter.read<String>(5, origin.world.name)

        val location = Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)

        return listOf(location.toTarget())
    }

    override fun showAFrame(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Float>(3, 0.0f)
        val pitch = parameter.read<Float>(4, 0.0f)
        val world = parameter.read<String>(5, origin.world.name)

        val location = Location(Bukkit.getWorld(world), x, y, z, yaw, pitch)

        return listOf(location)
    }

}
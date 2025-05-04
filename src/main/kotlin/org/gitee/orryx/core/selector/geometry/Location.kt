package org.gitee.orryx.core.selector.geometry

import org.bukkit.Bukkit
import org.bukkit.Location
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

object Location: ISelectorGeometry {

    override val keys = arrayOf("location")

    override val wiki: Selector
        get() = Selector.new("具体坐标点", keys, SelectorType.GEOMETRY)
            .addExample("@location 2 3 5 1 2")
            .addParm(Type.DOUBLE, "x", "0.0")
            .addParm(Type.DOUBLE, "y", "0.0")
            .addParm(Type.DOUBLE, "z", "0.0")
            .addParm(Type.FLOAT, "yaw", "0.0")
            .addParm(Type.FLOAT, "pitch", "0.0")
            .addParm(Type.STRING, "world", "原点世界")
            .description("具体坐标点")

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

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val x = parameter.read<Double>(0, 0.0)
        val y = parameter.read<Double>(1, 0.0)
        val z = parameter.read<Double>(2, 0.0)
        val yaw = parameter.read<Float>(3, 0.0f)
        val pitch = parameter.read<Float>(4, 0.0f)
        val world = parameter.read<String>(5, origin.world.name)

        val location = taboolib.common.util.Location(world, x, y, z, yaw, pitch)

        return listOf(location)
    }
}
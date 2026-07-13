package org.gitee.orryx.core.selector.geometry

import org.bukkit.util.Vector
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext

object Line: ISelectorGeometry {

    override val keys = arrayOf("line")

    override val wiki: Selector
        get() = Selector.new("线段范围", keys, SelectorType.GEOMETRY)
            .addExample("@line 10 1 2")
            .addParm(Type.DOUBLE, "长度", "10.0")
            .addParm(Type.DOUBLE, "宽度", "1.0")
            .addParm(Type.DOUBLE, "高度", "2.0")
            .addParm(Type.BOOLEAN, "跟随pitch", "false")
            .description("沿朝向方向的窄长方体，前方偏移固定为长度/2，适合剑气、直线冲击波类技能")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val location = origin.eyeLocation
        val long = parameter.read<Double>(0, 10.0)
        val wide = parameter.read<Double>(1, 1.0)
        val high = parameter.read<Double>(2, 2.0)
        val pitch = parameter.read<Boolean>(3, false)

        if (!pitch) { location.pitch = 0f }

        val forward = long / 2.0
        val axes = GeometryBroadPhase.basis(location.direction) ?: return emptyList()
        val center = location.toVector().add(axes[0].clone().multiply(forward))
        val box = GeometryBroadPhase.orientedBox(
            center,
            axes,
            Vector(long / 2.0, high / 2.0, wide / 2.0)
        )
        val bounds = GeometryBroadPhase.Bounds(center, box.broadHalfExtents)

        return GeometryBroadPhase.nearbyLivingEntities(origin.world, bounds)
            .filter { box.intersects(GeometryBroadPhase.entityBounds(it)) }
            .map { it.toTarget() }
            .toList()
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val location = origin.eyeLocation
        val long = parameter.read<Double>(0, 10.0)
        val wide = parameter.read<Double>(1, 1.0)
        val high = parameter.read<Double>(2, 2.0)
        val pitch = parameter.read<Boolean>(3, false)

        if (!pitch) { location.pitch = 0f }

        val forward = long / 2

        val list = mutableListOf<taboolib.common.util.Location>()

        val axes = GeometryBroadPhase.basis(location.direction) ?: return emptyList()
        val vectorX1 = axes[0]
        val vectorY1 = axes[1]
        val vectorZ1 = axes[2]

        val upFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val upBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val upBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

        val downFLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downFRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward + long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(wide / 2)))
        val downBLeft = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(- wide / 2)))
        val downBRight = adaptLocation(location.clone().add(vectorX1.clone().multiply(forward - long / 2)).add(vectorY1.clone().multiply(- high / 2)).add(vectorZ1.clone().multiply(wide / 2)))

        //上顶面
        list += createLine(upFLeft, upFRight).calculateLocations()
        list += createLine(upBLeft, upBRight).calculateLocations()
        list += createLine(upFLeft, upBLeft).calculateLocations()
        list += createLine(upFRight, upBRight).calculateLocations()

        //下面
        list += createLine(downFLeft, downFRight).calculateLocations()
        list += createLine(downBLeft, downBRight).calculateLocations()
        list += createLine(downFLeft, downBLeft).calculateLocations()
        list += createLine(downFRight, downBRight).calculateLocations()

        //四柱
        list += createLine(upFLeft, downFLeft).calculateLocations()
        list += createLine(upFRight, downFRight).calculateLocations()
        list += createLine(upBLeft, downBLeft).calculateLocations()
        list += createLine(upBRight, downBRight).calculateLocations()

        return list
    }
}

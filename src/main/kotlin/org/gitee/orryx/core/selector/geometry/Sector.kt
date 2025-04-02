package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.core.wiki.Selector
import org.gitee.orryx.core.wiki.SelectorType
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.module.effect.createArc
import taboolib.module.effect.createLine
import taboolib.module.kether.ScriptContext

object Sector: ISelectorGeometry {

    override val keys = arrayOf("sec", "sector")

    override val wiki: Selector
        get() = Selector.new("扇形范围", keys, SelectorType.GEOMETRY)
            .addExample("@sector 10 120 2 0")
            .addExample("@sec 10 120 2")
            .addParm(Type.DOUBLE, "半径", "10.0")
            .addParm(Type.DOUBLE, "角度", "120.0")
            .addParm(Type.DOUBLE, "高度", "2")
            .addParm(Type.DOUBLE, "y轴偏移", "0.0")
            .description("前方扇形范围的实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)
        val angle = parameter.read<Double>(1, 120.0)
        val h = parameter.read<Double>(2, 2.0)
        val offsetY = parameter.read<Double>(3, 0.0)

        val entities = origin.world.getNearbyEntities(origin.eyeLocation, r, r, r)
        val dir = origin.location.direction.clone().normalize()

        return entities.mapNotNull {
            if (it.location.y <= origin.eyeLocation.y + h/2 + offsetY && it.location.y + it.height >= origin.eyeLocation.y - h/2 + offsetY) {
                val vec = it.location.clone().apply { y = 0.0 }.toVector().subtract(origin.eyeLocation.clone().apply { y = 0.0 }.toVector())
                if (dir.angle(vec) >= angle/2) {
                    it.toTarget()
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val r = parameter.read<Double>(0, 10.0)
        val angle = parameter.read<Double>(1, 120.0)
        val h = parameter.read<Double>(2, 2.0)
        val offsetY = parameter.read<Double>(3, 0.0)
        val dir = origin.location.direction.clone().normalize()

        val up = createArc(
            adaptLocation(origin.eyeLocation.clone().apply { y += (h/2 + offsetY) }),
            startAngle = -angle/2,
            angle = angle,
            radius = r
        ).calculateLocations()
        val down = createArc(
            adaptLocation(origin.eyeLocation.clone().apply { y += (-h/2 + offsetY) }),
            startAngle = -angle/2,
            angle = angle,
            radius = r
        ).calculateLocations()

        val left = dir.clone().rotateAroundY(-angle/2).multiply(r)
        val right = dir.clone().rotateAroundY(angle/2).multiply(r)

        val leftUp = createLine(
            adaptLocation(origin.eyeLocation.clone().apply { y += (h/2 + offsetY) }),
            adaptLocation(origin.eyeLocation.clone().apply { y += (h/2 + offsetY) }.add(left))
        ).calculateLocations()
        val rightUp = createLine(
            adaptLocation(origin.eyeLocation.clone().apply { y += (h/2 + offsetY) }),
            adaptLocation(origin.eyeLocation.clone().apply { y += (h/2 + offsetY) }.add(right))
        ).calculateLocations()
        val leftDown = createLine(
            adaptLocation(origin.eyeLocation.clone().apply { y += (-h/2 + offsetY) }),
            adaptLocation(origin.eyeLocation.clone().apply { y += (-h/2 + offsetY) }.add(left))
        ).calculateLocations()
        val rightDown = createLine(
            adaptLocation(origin.eyeLocation.clone().apply { y += (-h/2 + offsetY) }),
            adaptLocation(origin.eyeLocation.clone().apply { y += (-h/2 + offsetY) }.add(right))
        ).calculateLocations()

        return up + down + leftUp + rightUp + leftDown + rightDown
    }

}
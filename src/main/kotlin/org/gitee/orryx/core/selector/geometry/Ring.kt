package org.gitee.orryx.core.selector.geometry

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
import taboolib.common.util.Location
import taboolib.module.effect.createCircle
import taboolib.module.kether.ScriptContext
import kotlin.math.cos
import kotlin.math.sin

object Ring: ISelectorGeometry {

    override val keys = arrayOf("ring")

    override val wiki: Selector
        get() = Selector.new("环形多点", keys, SelectorType.GEOMETRY)
            .addExample("@ring 5 8")
            .addParm(Type.DOUBLE, "半径", "5.0")
            .addParm(Type.INT, "数量", "8")
            .addParm(Type.DOUBLE, "y轴偏移", "0.0")
            .description("在原点周围等间距生成N个位置点（圆周均匀分布），适合环形法阵、召唤阵等技能")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val radius = parameter.read<Double>(0, 5.0)
        val amount = parameter.read<Int>(1, 8)
        val offsetY = parameter.read<Double>(2, 0.0)

        val baseLoc = origin.location.clone()
        baseLoc.y += offsetY

        return (0 until amount).map { i ->
            val angle = 2 * Math.PI / amount * i
            val x = cos(angle) * radius
            val z = sin(angle) * radius
            baseLoc.clone().add(x, 0.0, z).toTarget()
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val radius = parameter.read<Double>(0, 5.0)
        val offsetY = parameter.read<Double>(2, 0.0)

        val baseLoc = origin.location.clone()
        baseLoc.y += offsetY

        return createCircle(adaptLocation(baseLoc), radius, 5.0, 0).calculateLocations()
    }
}

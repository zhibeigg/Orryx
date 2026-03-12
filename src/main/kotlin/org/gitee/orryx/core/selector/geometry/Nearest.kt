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
import taboolib.module.effect.createSphere
import taboolib.module.kether.ScriptContext

object Nearest: ISelectorGeometry {

    override val keys = arrayOf("nearest")

    override val wiki: Selector
        get() = Selector.new("最近实体", keys, SelectorType.GEOMETRY)
            .addExample("@nearest 3 32")
            .addParm(Type.INT, "数量", "1")
            .addParm(Type.DOUBLE, "搜索半径", "32.0")
            .description("选取距离原点最近的N个实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val amount = parameter.read<Int>(0, 1)
        val radius = parameter.read<Double>(1, 32.0)

        val originLoc = origin.location
        val entities = origin.world.getNearbyEntities(originLoc, radius, radius, radius)

        return entities
            .filter { it.location != originLoc }
            .sortedBy { it.location.distanceSquared(originLoc) }
            .take(amount)
            .map { it.toTarget() }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        val origin = context.getParameter().origin ?: return emptyList()

        val radius = parameter.read<Double>(1, 32.0)

        return createSphere(adaptLocation(origin.eyeLocation), radius = radius).calculateLocations().map { it }
    }
}

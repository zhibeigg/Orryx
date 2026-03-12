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
import taboolib.common.util.Location
import taboolib.module.kether.ScriptContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object Scatter: ISelectorGeometry {

    override val keys = arrayOf("scatter")

    override val wiki: Selector
        get() = Selector.new("散射多点", keys, SelectorType.GEOMETRY)
            .addExample("@scatter 5 10")
            .addParm(Type.INT, "数量", "5")
            .addParm(Type.DOUBLE, "半径", "10.0")
            .addParm(Type.DOUBLE, "前方偏移", "0.0")
            .description("在指定范围内随机生成N个位置点，适合陨石雨、随机落雷等AOE技能")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()

        val amount = parameter.read<Int>(0, 5)
        val radius = parameter.read<Double>(1, 10.0)
        val forward = parameter.read<Double>(2, 0.0)

        val baseLoc = origin.location.clone()
        if (forward != 0.0) {
            val dir = origin.eyeLocation.direction.clone().normalize()
            dir.setY(0).normalize()
            baseLoc.add(dir.multiply(forward))
        }

        return (1..amount).map {
            // 在圆形范围内随机采样
            val angle = Random.nextDouble() * 2 * Math.PI
            val dist = Random.nextDouble() * radius
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            baseLoc.clone().add(x, 0.0, z).toTarget()
        }
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        // 散射点每次随机，粒子渲染返回空即可
        return emptyList()
    }
}

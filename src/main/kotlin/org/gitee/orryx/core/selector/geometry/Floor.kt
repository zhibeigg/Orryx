package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.floor
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.read
import org.gitee.orryx.utils.toTarget
import taboolib.common.platform.function.adaptLocation
import taboolib.module.kether.ScriptContext

object Floor: ISelectorGeometry {

    override val keys = arrayOf("floor")

    override val wiki: Selector
        get() = Selector.new("脚下地面位置", keys, SelectorType.GEOMETRY)
            .addExample("@floor 10")
            .addParm(Type.DOUBLE, "最低限度", "50.0")
            .description("选中脚下的第一个碰撞方块(最低支持1.12.2版本)")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        val origin = context.getParameter().origin ?: return emptyList()
        val distance = parameter.read<Double>(0, 50.0)

        return listOf(floor(origin.location.clone(), distance).first.toTarget())
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<taboolib.common.util.Location> {
        val origin = context.getParameter().origin ?: return emptyList()
        val distance = parameter.read<Double>(0, 50.0)

        return listOf(adaptLocation(floor(origin.location.clone(), distance).first))
    }
}
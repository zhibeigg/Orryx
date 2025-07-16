package org.gitee.orryx.core.selector.geometry

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorGeometry
import org.gitee.orryx.core.targets.ITarget
import org.gitee.orryx.module.wiki.Selector
import org.gitee.orryx.module.wiki.SelectorType
import org.gitee.orryx.module.wiki.Type
import taboolib.common.util.Location
import taboolib.module.kether.ScriptContext

object Frustum: ISelectorGeometry {

    override val keys = arrayOf("frustum")

    override val wiki: Selector
        get() = Selector.new("圆台形范围", keys, SelectorType.GEOMETRY)
            .addExample("@frustum 1 5 10 0 1 false")
            .addParm(Type.DOUBLE, "上半径", "1.0")
            .addParm(Type.DOUBLE, "下半径", "10.0")
            .addParm(Type.DOUBLE, "仰角", "10.0")
            .addParm(Type.DOUBLE, "偏航角", "0")
            .addParm(Type.DOUBLE, "y轴偏移", "0.0")
            .addParm(Type.BOOLEAN, "跟随pitch", "false")
            .description("前方扇形范围的实体")

    override fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>> {
        TODO("Not yet implemented")
    }

    override fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location> {
        TODO("Not yet implemented")
    }
}
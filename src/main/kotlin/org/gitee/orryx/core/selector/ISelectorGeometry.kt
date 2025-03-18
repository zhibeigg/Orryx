package org.gitee.orryx.core.selector

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.common.util.Location
import taboolib.module.kether.ScriptContext

interface ISelectorGeometry: ISelector, WikiSelector {

    /**
     * 获得目标
     * */
    fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>>

    /**
     * 获得一帧可视粒子渲染位置列表
     * */
    fun aFrameShowLocations(context: ScriptContext, parameter: StringParser.Entry): List<Location>

}
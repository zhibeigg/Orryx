package org.gitee.orryx.core.selector

import org.bukkit.Location
import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.module.kether.ScriptContext

interface ISelectorGeometry: ISelector {

    /**
     * 获得目标
     * */
    fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>>

    /**
     * 获得一帧可视粒子渲染位置列表
     * */
    fun showAFrame(context: ScriptContext, parameter: StringParser.Entry): List<Location>

}
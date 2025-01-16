package org.gitee.orryx.core.selector

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.module.kether.ScriptContext

interface ISelectorGeometry: ISelector {

    fun getTargets(context: ScriptContext, parameter: StringParser.Entry): List<ITarget<*>>

    fun showAFrame(context: ScriptContext, parameter: StringParser.Entry)

}
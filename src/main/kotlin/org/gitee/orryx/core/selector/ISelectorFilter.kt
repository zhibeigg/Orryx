package org.gitee.orryx.core.selector

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.targets.ITarget
import taboolib.module.kether.ScriptContext

interface ISelectorFilter: ISelector {

    /**
     * 过滤目标
     * */
    fun filter(target: ITarget<*>, context: ScriptContext, parameter: StringParser.Entry): Boolean

}
package org.gitee.orryx.utils

import org.gitee.orryx.core.parser.StringParser
import org.gitee.orryx.core.selector.ISelectorFilter
import org.gitee.orryx.core.targets.ITarget
import taboolib.module.kether.ScriptContext

internal fun Iterable<ITarget<*>>.filter(selector: ISelectorFilter, context: ScriptContext, parameter: StringParser.Entry): Iterable<ITarget<*>> {
    return filter { target -> selector.filter(target, context, parameter) }
}

internal fun MutableSet<ITarget<*>>.removeIf(selector: ISelectorFilter, context: ScriptContext, parameter: StringParser.Entry) {
    removeIf { target -> !selector.filter(target, context, parameter) }
}
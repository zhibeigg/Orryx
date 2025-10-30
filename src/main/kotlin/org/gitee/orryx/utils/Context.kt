package org.gitee.orryx.utils

import org.gitee.orryx.core.kether.parameter.IParameter
import taboolib.module.kether.ScriptContext

const val PARAMETER = "Orryx@Parameter"

/**
 * @throws IllegalStateException Parameter not found
 * */
internal fun ScriptContext.getParameter() : IParameter {
    return this.get<IParameter>(PARAMETER) ?: error("Parameter not found")
}

internal fun ScriptContext.getParameterOrNull() : IParameter {
    return this.get<IParameter>(PARAMETER)
}
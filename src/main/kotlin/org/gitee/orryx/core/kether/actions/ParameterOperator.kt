package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.parameter.IParameter
import taboolib.module.kether.ScriptContext

class ParameterOperator(
    var reader: Reader? = null,
    var writer: Writer? = null,
    val usable: Array<Method> = arrayOf(Method.INCREASE, Method.DECREASE, Method.MODIFY),
) {

    class Reader(val func: (IParameter) -> Any?)

    class Writer(val func: (IParameter, Method, ScriptContext, Any?) -> Unit)

    enum class Method {
        INCREASE, DECREASE, MODIFY, NONE
    }

}
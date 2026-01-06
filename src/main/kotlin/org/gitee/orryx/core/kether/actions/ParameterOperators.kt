package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.utils.firstInstanceOrNull
import org.gitee.orryx.utils.readContainer
import taboolib.module.kether.ScriptContext
import java.util.concurrent.CompletableFuture

enum class ParameterOperators(
    val reader: ((IParameter) -> Any?)? = null,
    val writer: ((IParameter, ParameterOperator.Method, ScriptContext, Any?, CompletableFuture<Any?>) -> Unit)? = null,
    vararg val usable: ParameterOperator.Method,
) {

    ORIGIN(
        { it.origin },
        { parm, _, c, any, f ->
            any.readContainer(c)?.thenAccept {
                it.firstInstanceOrNull<ITargetLocation<*>>()?.let { origin ->
                    parm.origin = origin
                    f.complete(parm.origin)
                }
            }
        },
        ParameterOperator.Method.MODIFY
    ),

    SKILL({ (it as SkillParameter).skill }),

    LEVEL({ (it as SkillParameter).level }),

    STATION({ (it as StationParameter<*>).stationLoader });

    fun build() = ParameterOperator(
        reader = if (reader != null) ParameterOperator.Reader(reader) else null,
        writer = if (writer != null) ParameterOperator.Writer(writer) else null,
        usable = arrayOf(*usable)
    )
}
package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.NAMESPACE
import org.gitee.orryx.utils.getParameter
import taboolib.module.kether.KetherParser
import taboolib.module.kether.script

object VariableActions {

    @KetherParser(["lazy"], namespace = NAMESPACE)
    private fun actionLazy() = combinationParser(
        Action.new("Variable懒变量", "懒变量", "lazy")
            .description("以懒加载方式加载变量(第一次加载后保存数据，后续直接调用)")
            .addEntry("加载的变量名(大小写不敏感)", Type.STRING)
            .result("变量值", Type.ANY)
    ) {
        it.group(
            text()
        ).apply(it) { key ->
            now { script().getParameter().getVariable(key.uppercase(), true) }
        }
    }

    @KetherParser(["reinit"], namespace = NAMESPACE)
    private fun actionReinit() = combinationParser(
        Action.new("Variable懒变量", "重置懒变量", "reinit")
            .description("重新初始化变量，返回实时值")
            .addEntry("加载的变量名(大小写不敏感)", Type.STRING)
            .result("变量值", Type.ANY)
    ) {
        it.group(
            text()
        ).apply(it) { key ->
            now { script().getParameter().getVariable(key.uppercase(), false) }
        }
    }

}
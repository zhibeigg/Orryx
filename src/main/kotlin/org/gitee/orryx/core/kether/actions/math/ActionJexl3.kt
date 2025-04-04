package org.gitee.orryx.core.kether.actions.math

import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.MapContext
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.getParameterOrNull
import taboolib.common.util.unsafeLazy
import taboolib.library.kether.QuestContext.Frame
import taboolib.module.kether.*

/**
 * TabooLib
 * taboolib.module.kether.action.transform.ActionJexl3
 *
 * @author 坏黑
 * @since 2022/9/3 16:03
 */
object ActionJexl3 {

    internal val jexl: JexlEngine by unsafeLazy { JexlBuilder().cache(256).create() }

    /**
     * calc dynamic ""
     * calc ""
     */
    @KetherParser(["calc", "calculate"], namespace = ORRYX_NAMESPACE)
    fun actionCalc() = scriptParser(
        arrayOf(
            Action.new("Math数学运算", "jexl表达式缓存", "calc", false)
                .description("jexl表达式缓存，对重复使用的表达式进行缓存，可使用上下文中存储的key值")
                .addEntry("运算公式", Type.STRING, false, head = "dynamic")
                .result("计算结果", Type.ANY)
        )
    ) {
        it.mark()
        try {
            it.expects("dynamic")
            val expression = it.nextParsedAction()
            actionTake { run(expression).str { exp -> jexl.createExpression(exp).evaluate(createMapContext()) } }
        } catch (ex: Throwable) {
            it.reset()
            val expression = jexl.createExpression(it.nextToken())
            actionNow { expression.evaluate(createMapContext()) }
        }
    }

    /**
     * invoke dynamic ""
     * invoke ""
     */
    @KetherParser(["invoke"], namespace = ORRYX_NAMESPACE)
    fun actionInvoke() = scriptParser(
        arrayOf(
            Action.new("Math数学运算", "jexl预编译脚本", "invoke", false)
                .description("jexl脚本解析，高频计算使用，可使用上下文中存储的key值")
                .addEntry("脚本公式", Type.STRING, false, head = "dynamic")
                .result("计算结果", Type.ANY)
        )
    ) {
        it.mark()
        try {
            it.expects("dynamic")
            val script = it.nextParsedAction()
            actionTake { run(script).str { exp -> jexl.createScript(exp).execute(createMapContext()) } }
        } catch (ex: Throwable) {
            it.reset()
            val script = jexl.createScript(it.nextToken())
            actionNow { script.execute(createMapContext()) }
        }
    }

    private fun Frame.createMapContext(): MapContext {
        return script().getParameterOrNull()?.getVariable("Jexl3Context", MapContext(deepVars())) as? MapContext ?: MapContext(deepVars())
    }

}
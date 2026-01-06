package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.adaptCommandSender
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionFuture
import taboolib.module.kether.actionNow
import taboolib.module.kether.run
import taboolib.module.kether.script
import kotlin.run

object ContextActions {

    private val registerOperators = mutableMapOf<String, ParameterOperator>()

    init {
        ParameterOperators.entries.forEach {
            registerOperators[it.name] = it.build()
        }
    }

    @KetherParser(["senderSpace"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun senderSpace() = combinationParser(
        Action.new("上下文", "sender空间", "senderSpace", true)
            .description("循环以指定sender执行内部语句")
            .addContainerEntry("需要参与的Senders")
            .addEntry("执行语句 { action }", Type.ANY)
    ) {
        it.group(
            theyContainer(),
            action()
        ).apply(it) { container, action ->
            now {
                val sender = script().sender
                container?.forEachInstance<ITargetEntity<*>> { target ->
                    script().sender =
                        target.entity.getBukkitLivingEntity()?.let { entity -> adaptCommandSender(entity) }
                            ?: return@forEachInstance
                    run(action)
                }
                script().sender = sender
            }
        }
    }

    @KetherParser(["parameter", "parm"], namespace = ORRYX_NAMESPACE)
    private fun parameter() = scriptParser(
        Action.new("上下文", "读取parameter参数", "parameter/parm")
            .description("获取指定parameter参数")
            .addEntry("目标参数", Type.STRING)
            .result("获取的参数", Type.ANY),
        Action.new("上下文", "设置parameter参数", "parameter/parm")
            .description("设置指定parameter参数")
            .addEntry("目标参数", Type.STRING)
            .addEntry("设置标识符", Type.SYMBOL, head = "set/to/=")
            .addEntry("目标参数", Type.ANY),
        Action.new("上下文", "加parameter参数", "parameter/parm")
            .description("加指定parameter参数")
            .addEntry("目标参数", Type.STRING)
            .addEntry("加标识符", Type.SYMBOL, head = "add/increase/+")
            .addEntry("目标参数", Type.ANY),
        Action.new("上下文", "减parameter参数", "parameter/parm")
            .description("减指定parameter参数")
            .addEntry("目标参数", Type.STRING)
            .addEntry("减标识符", Type.SYMBOL, head = "sub/decrease/-")
            .addEntry("目标参数", Type.ANY)
    ) {
        val key = it.nextToken()
        val operator = registerOperators.entries.first { (name, _) ->
            name == key.uppercase()
        }.value
        it.mark()
        val method = when (it.nextToken()) {
            "set", "to", "=" -> ParameterOperator.Method.MODIFY
            "add", "increase", "+" -> ParameterOperator.Method.INCREASE
            "sub", "decrease", "-" -> ParameterOperator.Method.DECREASE
            else -> {
                it.reset()
                ParameterOperator.Method.NONE
            }
        }
        if (method == ParameterOperator.Method.NONE) {
            actionNow {
                operator.reader!!.func(script().getParameter())
            }
        } else {
            if (operator.usable.contains(method)) {
                val value = it.nextParsedAction()
                actionFuture { f ->
                    run(value).thenApply { value ->
                        operator.writer!!.func(script().getParameter(), method, script(), value, f)
                    }
                }
            } else {
                error("$method not supported in $key")
            }
        }
    }
}
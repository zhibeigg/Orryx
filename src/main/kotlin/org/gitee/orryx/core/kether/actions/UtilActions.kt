package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.command.OrryxTestCommand
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.nextHeadActionOrNull
import org.gitee.orryx.utils.scriptParser
import taboolib.library.kether.ParsedAction
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionFuture
import taboolib.module.kether.actionNow
import taboolib.module.kether.run
import java.util.concurrent.CompletableFuture

object UtilActions {

    private fun isBlankValue(value: Any?): Boolean {
        return value == null || (value is CharSequence && value.isBlank())
    }

    private fun isEmptyValue(value: Any?): Boolean {
        return when (value) {
            null -> true
            is CharSequence -> value.isEmpty()
            is Collection<*> -> value.isEmpty()
            is Map<*, *> -> value.isEmpty()
            is Iterable<*> -> !value.iterator().hasNext()
            is Array<*> -> value.isEmpty()
            is BooleanArray -> value.isEmpty()
            is ByteArray -> value.isEmpty()
            is CharArray -> value.isEmpty()
            is DoubleArray -> value.isEmpty()
            is FloatArray -> value.isEmpty()
            is IntArray -> value.isEmpty()
            is LongArray -> value.isEmpty()
            is ShortArray -> value.isEmpty()
            else -> false
        }
    }

    private fun toBooleanValue(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            null -> false
            else -> value.toString().equals("true", true)
        }
    }

    @KetherParser(["contains"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionContains() = scriptParser(
        Action.new("Util工具类", "是否包含", "contains", true)
            .description("Iterable 或 String 是否包含 value")
            .addEntry("Iterable 或 String", Type.ANY)
            .addEntry("value", Type.ANY)
            .result("是否包含", Type.BOOLEAN)
    ) {
        val check = it.nextParsedAction()
        val value = it.nextParsedAction()
        actionFuture { future ->
            run(check).whenComplete { checkValue, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                run(value).whenComplete { valueValue, ex2 ->
                    if (ex2 != null) {
                        future.completeExceptionally(ex2)
                    } else {
                        future.complete(
                            when (checkValue) {
                                is Iterable<*> -> checkValue.contains(valueValue)
                                is String -> checkValue.contains(valueValue.toString())
                                else -> false
                            }
                        )
                    }
                }
            }
        }
    }

    @KetherParser(["isNull"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionIsNull() = scriptParser(
        Action.new("Util工具类", "是否为空", "isNull", true)
            .addEntry("要检测的对象", Type.ANY)
            .result("是否为空", Type.BOOLEAN)
    ) {
        val value = it.nextParsedAction()
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                } else {
                    future.complete(v == null)
                }
            }
        }
    }

    @KetherParser(["notNull"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionNotNull() = scriptParser(
        Action.new("Util工具类", "是否非空", "notNull", true)
            .addEntry("要检测的对象", Type.ANY)
            .result("是否非空", Type.BOOLEAN)
    ) {
        val value = it.nextParsedAction()
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                } else {
                    future.complete(v != null)
                }
            }
        }
    }

    @KetherParser(["ifNull"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionIfNull() = scriptParser(
        Action.new("Util工具类", "空值默认", "ifNull", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("如果为 null 则返回", Type.ANY)
            .description("如果 参数1 为 null，则返回 参数2，否则返回 参数1（参数2 为惰性求值）")
            .result("参数", Type.ANY)
    ) {
        val primary = it.nextParsedAction()
        val fallback = it.nextParsedAction()
        actionFuture { future ->
            run(primary)
                .thenCompose { value ->
                    if (value != null) {
                        CompletableFuture.completedFuture(value)
                    } else {
                        run(fallback)
                    }
                }
                .whenComplete { value, ex ->
                    if (ex != null) {
                        future.completeExceptionally(ex)
                    } else {
                        future.complete(value)
                    }
                }
        }
    }

    @KetherParser(["unlessNull"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionUnlessNull() = scriptParser(
        Action.new("Util工具类", "非空执行", "unlessNull", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("非空时执行的语句", Type.ANY)
            .addEntry("空值时执行的语句", Type.ANY, optional = true, head = "else")
            .description("参数1 不为 null 时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）")
            .result("执行结果", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val thenAction = it.nextParsedAction()
        val elseAction = it.nextHeadActionOrNull(arrayOf("else"))
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                if (v != null) {
                    variables()["@value"] = v
                    run(thenAction).whenComplete { result, ex2 ->
                        variables().remove("@value")
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else if (elseAction != null) {
                    run(elseAction).whenComplete { result, ex2 ->
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else {
                    future.complete(null)
                }
            }
        }
    }

    @KetherParser(["coalesce"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionCoalesce() = scriptParser(
        Action.new("Util工具类", "取第一个非空", "coalesce", true)
            .addEntry("候选值 1", Type.ANY)
            .addEntry("候选值 N", Type.ANY, optional = true, head = "or")
            .description("从左到右依次求值，返回第一个不为 null 的结果；可以使用 `or` 连接多个候选值")
            .result("第一个非空值", Type.ANY)
    ) {
        val actions = mutableListOf<ParsedAction<*>>()
        actions += it.nextParsedAction()
        while (true) {
            val next = it.nextHeadActionOrNull(arrayOf("or")) ?: break
            actions += next
        }
        actionFuture { future ->
            fun eval(index: Int) {
                if (index >= actions.size) {
                    future.complete(null)
                    return
                }
                run(actions[index]).whenComplete { v, ex ->
                    if (ex != null) {
                        future.completeExceptionally(ex)
                    } else if (v != null) {
                        future.complete(v)
                    } else {
                        eval(index + 1)
                    }
                }
            }
            eval(0)
        }
    }

    @KetherParser(["ifEmpty"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionIfEmpty() = scriptParser(
        Action.new("Util工具类", "空集合默认", "ifEmpty", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("如果为空则返回", Type.ANY)
            .description("如果 参数1 为空（null/空字符串/空集合等），则返回 参数2，否则返回 参数1（参数2 为惰性求值）")
            .result("参数", Type.ANY)
    ) {
        val primary = it.nextParsedAction()
        val fallback = it.nextParsedAction()
        actionFuture { future ->
            run(primary)
                .thenCompose { value ->
                    if (isEmptyValue(value)) {
                        run(fallback)
                    } else {
                        CompletableFuture.completedFuture(value)
                    }
                }
                .whenComplete { value, ex ->
                    if (ex != null) {
                        future.completeExceptionally(ex)
                    } else {
                        future.complete(value)
                    }
                }
        }
    }

    @KetherParser(["unlessEmpty"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionUnlessEmpty() = scriptParser(
        Action.new("Util工具类", "非空集合执行", "unlessEmpty", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("非空时执行的语句", Type.ANY)
            .addEntry("空时执行的语句", Type.ANY, optional = true, head = "else")
            .description("参数1 非空时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）")
            .result("执行结果", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val thenAction = it.nextParsedAction()
        val elseAction = it.nextHeadActionOrNull(arrayOf("else"))
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                if (!isEmptyValue(v)) {
                    variables()["@value"] = v
                    run(thenAction).whenComplete { result, ex2 ->
                        variables().remove("@value")
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else if (elseAction != null) {
                    run(elseAction).whenComplete { result, ex2 ->
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else {
                    future.complete(null)
                }
            }
        }
    }

    @KetherParser(["ifBlank"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionIfBlank() = scriptParser(
        Action.new("Util工具类", "空白默认", "ifBlank", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("如果为空白则返回", Type.ANY)
            .description("如果 参数1 为空白（null/空字符串/全空格），则返回 参数2，否则返回 参数1（参数2 为惰性求值）")
            .result("参数", Type.ANY)
    ) {
        val primary = it.nextParsedAction()
        val fallback = it.nextParsedAction()
        actionFuture { future ->
            run(primary)
                .thenCompose { value ->
                    if (isBlankValue(value)) {
                        run(fallback)
                    } else {
                        CompletableFuture.completedFuture(value)
                    }
                }
                .whenComplete { value, ex ->
                    if (ex != null) {
                        future.completeExceptionally(ex)
                    } else {
                        future.complete(value)
                    }
                }
        }
    }

    @KetherParser(["unlessBlank"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionUnlessBlank() = scriptParser(
        Action.new("Util工具类", "非空白执行", "unlessBlank", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("非空白时执行的语句", Type.ANY)
            .addEntry("空白时执行的语句", Type.ANY, optional = true, head = "else")
            .description("参数1 非空白时，将其写入变量 @value 并执行 参数2；否则执行 else 后的语句（若未提供则返回 null）")
            .result("执行结果", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val thenAction = it.nextParsedAction()
        val elseAction = it.nextHeadActionOrNull(arrayOf("else"))
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                if (!isBlankValue(v)) {
                    variables()["@value"] = v
                    run(thenAction).whenComplete { result, ex2 ->
                        variables().remove("@value")
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else if (elseAction != null) {
                    run(elseAction).whenComplete { result, ex2 ->
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.complete(result)
                        }
                    }
                } else {
                    future.complete(null)
                }
            }
        }
    }

    @KetherParser(["require"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionRequire() = scriptParser(
        Action.new("Util工具类", "断言为真", "require", true)
            .addEntry("条件", Type.BOOLEAN)
            .addEntry("失败信息", Type.STRING)
            .description("若条件为 false，则抛出异常中断脚本")
            .result("是否通过", Type.BOOLEAN)
    ) {
        val condition = it.nextParsedAction()
        val message = it.nextParsedAction()
        actionFuture { future ->
            run(condition).whenComplete { okAny, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                val ok = toBooleanValue(okAny)
                if (ok) {
                    future.complete(true)
                    return@whenComplete
                }
                run(message).whenComplete { msgAny, ex2 ->
                    if (ex2 != null) {
                        future.completeExceptionally(ex2)
                    } else {
                        future.completeExceptionally(IllegalStateException(msgAny?.toString() ?: "require failed"))
                    }
                }
            }
        }
    }

    @KetherParser(["requireNotNull"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionRequireNotNull() = scriptParser(
        Action.new("Util工具类", "断言非空", "requireNotNull", true)
            .addEntry("要检测的对象", Type.ANY)
            .addEntry("失败信息", Type.STRING)
            .description("若参数为 null，则抛出异常中断脚本，否则返回该参数")
            .result("参数", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val message = it.nextParsedAction()
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                if (v != null) {
                    future.complete(v)
                } else {
                    run(message).whenComplete { msgAny, ex2 ->
                        if (ex2 != null) {
                            future.completeExceptionally(ex2)
                        } else {
                            future.completeExceptionally(IllegalStateException(msgAny?.toString() ?: "requireNotNull failed"))
                        }
                    }
                }
            }
        }
    }

    @KetherParser(["tap"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionTap() = scriptParser(
        Action.new("Util工具类", "旁路执行", "tap", true)
            .addEntry("原始值", Type.ANY)
            .addEntry("旁路语句", Type.ANY)
            .description("先求值参数1，写入变量 @value，执行参数2（忽略其返回值），最后返回参数1")
            .result("原始值", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val sideEffect = it.nextParsedAction()
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                variables()["@value"] = v
                run(sideEffect).whenComplete { _, ex2 ->
                    variables().remove("@value")
                    if (ex2 != null) {
                        future.completeExceptionally(ex2)
                    } else {
                        future.complete(v)
                    }
                }
            }
        }
    }

    @KetherParser(["let"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionLet() = scriptParser(
        Action.new("Util工具类", "带值执行", "let", true)
            .addEntry("原始值", Type.ANY)
            .addEntry("执行语句", Type.ANY)
            .description("先求值参数1，写入变量 @value，执行参数2，并返回参数2的结果")
            .result("执行结果", Type.ANY)
    ) {
        val value = it.nextParsedAction()
        val body = it.nextParsedAction()
        actionFuture { future ->
            run(value).whenComplete { v, ex ->
                if (ex != null) {
                    future.completeExceptionally(ex)
                    return@whenComplete
                }
                variables()["@value"] = v
                run(body).whenComplete { result, ex2 ->
                    variables().remove("@value")
                    if (ex2 != null) {
                        future.completeExceptionally(ex2)
                    } else {
                        future.complete(result)
                    }
                }
            }
        }
    }

    @KetherParser(["isUnlimited"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionIsUnlimited() = scriptParser(
        Action.new("Util工具类", "是否无限模式", "isUnlimited", true)
            .description("检测当前玩家是否处于无限模式（unlimit），无限模式下施放技能不消耗资源")
            .result("是否处于无限模式", Type.BOOLEAN)
    ) {
        actionNow {
            OrryxTestCommand.isUnlimited(bukkitPlayer())
        }
    }
}

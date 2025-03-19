package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.warning
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object ContextActions {

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
                container.readContainer(script())?.forEachInstance<ITargetEntity<*>> { target ->
                    script().sender = target.entity.getBukkitLivingEntity()?.let { entity -> adaptCommandSender(entity) } ?: return@forEachInstance
                    run(action)
                }
                script().sender = sender
            }
        }
    }

    @KetherParser(["parameter", "parm"], namespace = ORRYX_NAMESPACE)
    private fun parameter() = combinationParser(
        Action.new("上下文", "parameter参数", "parameter/parm")
            .description("获取指定parameter参数")
            .addEntry("目标参数", Type.STRING)
    ) {
        it.group(
            text(),
            symbol().option(),
            action().option()
        ).apply(it) { key, symbol, action ->
            future {
                val parameter = script().getParameter()
                val method = Method.entries.find { method ->
                    symbol?.lowercase() in method.symbols
                } ?: Method.NONE
                val future = CompletableFuture<Any>()
                val value = action?.let { run(action).orNull() }
                    future.complete(
                        when(val pkey = key.uppercase()) {
                            "SKILL" -> parameter.parseParm(pkey, ParmType.SKILL, method, value, script())
                            "LEVEL" -> parameter.parseParm(pkey, ParmType.SKILL, method, value, script())
                            "STATION" -> parameter.parseParm(pkey, ParmType.STATION, method, value, script())
                            "ORIGIN" -> parameter.parseParm(pkey, ParmType.ALL, method, value, script())
                            else -> warning("not found parm $key")
                        }
                    )
                future
            }
        }
    }

    private fun IParameter.parseParm(key: String, type: ParmType, method: Method, value: Any?, context: ScriptContext): Any? {
        return when(type) {
            ParmType.SKILL -> {
                this as SkillParameter
                when(key) {
                    "SKILL" -> skill
                    "LEVEL" -> level
                    else -> null
                }
            }
            ParmType.STATION -> {
                this as StationParameter
                when(key) {
                    "STATION" -> stationLoader
                    else -> null
                }
            }
            ParmType.ALL -> {
                when(key) {
                    "ORIGIN" -> {
                        when(method) {
                            Method.INCREASE -> origin
                            Method.DECREASE -> origin
                            Method.MODIFY -> value.readContainer(context)?.firstInstanceOrNull<ITargetLocation<*>>()?.let { origin = it }
                            Method.NONE -> origin
                        }
                    }
                    else -> null
                }
            }
        }
    }

    enum class ParmType {
        SKILL, STATION, ALL;
    }

}
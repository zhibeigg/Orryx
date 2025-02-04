package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.targets.ITargetEntity
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.adaptCommandSender
import taboolib.common.platform.function.warning
import taboolib.module.kether.KetherParser
import taboolib.module.kether.run
import taboolib.module.kether.script

object ContextActions {

    @KetherParser(["senderSpace"], namespace = NAMESPACE, shared = true)
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

    @KetherParser(["parameter", "parm"], namespace = NAMESPACE)
    private fun parameter() = combinationParser(
        Action.new("上下文", "parameter参数", "parameter/parm")
            .description("获取指定parameter参数")
            .addEntry("目标参数", Type.STRING)
    ) {
        it.group(
            text()
        ).apply(it) { key ->
            now {
                val parameter = script().getParameter()
                return@now when(val pkey = key.uppercase()) {
                    "SKILL" -> parameter.parseParm(pkey, ParmType.SKILL)
                    "LEVEL" -> parameter.parseParm(pkey, ParmType.SKILL)
                    "STATION" -> parameter.parseParm(pkey, ParmType.STATION)
                    "ORIGIN" -> parameter.parseParm(pkey, ParmType.ALL)
                    else -> warning("not found parm $key")
                }
            }
        }
    }

    private fun IParameter.parseParm(key: String, type: ParmType): Any? {
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
                    "ORIGIN" -> origin
                    else -> null
                }
            }
        }
    }

    enum class ParmType {
        SKILL, STATION, ALL;
    }

}
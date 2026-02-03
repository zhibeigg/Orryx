package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.common.timer.StationTimer
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.nextHeadActionOrNull
import org.gitee.orryx.utils.scriptParser
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object CooldownActions {

    @KetherParser(["cooldown"], namespace = ORRYX_NAMESPACE)
    private fun actionCooldown() = scriptParser(
        Action.new("Cooldown冷却", "检测冷却", "cooldown")
            .description("检测技能/中转站是否在冷却中")
            .addEntry("检测标识符", type = Type.SYMBOL, head = "has")
            .addEntry("检测的技能/中转站", Type.STRING, true, "当前", "key")
            .addEntry("类型 skill/station", Type.STRING, true, "当前", "type")
            .result("是否在冷却中", Type.BOOLEAN),
        Action.new("Cooldown冷却", "给予冷却", "cooldown")
            .description("给予玩家技能/中转站冷却")
            .addEntry("给予标识符", type = Type.SYMBOL, head = "add")
            .addEntry("冷却值", type = Type.LONG)
            .addEntry("技能/中转站", Type.STRING, true, "当前", "key")
            .addEntry("类型 skill/station", Type.STRING, true, "当前", "type"),
        Action.new("Cooldown冷却", "减少冷却", "cooldown")
            .description("减少玩家技能/中转站冷却")
            .addEntry("减少标识符", type = Type.SYMBOL, head = "take")
            .addEntry("冷却值", type = Type.LONG)
            .addEntry("技能/中转站", Type.STRING, true, "当前", "key")
            .addEntry("类型 skill/station", Type.STRING, true, "当前", "type"),
        Action.new("Cooldown冷却", "设置冷却", "cooldown")
            .description("设置玩家技能/中转站冷却")
            .addEntry("设置标识符", type = Type.SYMBOL, head = "set/to")
            .addEntry("冷却值", type = Type.LONG)
            .addEntry("技能/中转站", Type.STRING, true, "当前", "key")
            .addEntry("类型 skill/station", Type.STRING, true, "当前", "type"),
        Action.new("Cooldown冷却", "重置冷却", "cooldown")
            .description("重置玩家技能/中转站冷却")
            .addEntry("重置标识符", type = Type.SYMBOL, head = "reset")
            .result("新的冷却值", Type.LONG),
        Action.new("Cooldown冷却", "获取倒计时", "cooldown")
            .description("获取玩家技能/中转站冷却倒计时")
            .addEntry("获取标识符", optional = true, type = Type.SYMBOL, head = "get/countdown")
            .addEntry("技能/中转站", Type.STRING, true, "当前", "key")
            .addEntry("类型 skill/station", Type.STRING, true, "当前", "type")
            .result("倒计时", Type.LONG)
    ) {
        it.switch {
            case("has") {
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")
                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        future.complete(!SkillTimer.hasNext(script().sender!!, skill))
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        future.complete(!StationTimer.hasNext(script().sender!!, station))
                    }
                }
            }
            case("add") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(SkillTimer.increase(script().sender!!, skill, cooldown * 50))
                        }
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(StationTimer.increase(script().sender!!, station, cooldown * 50))
                        }
                    }
                }
            }
            case("take") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(SkillTimer.reduce(script().sender!!, skill, cooldown * 50))
                        }
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(StationTimer.reduce(script().sender!!, station, cooldown * 50))
                        }
                    }
                }
            }
            case("set", "to") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(SkillTimer.set(script().sender!!, skill, cooldown * 50))
                        }
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { cooldown ->
                            future.complete(StationTimer.set(script().sender!!, station, cooldown * 50))
                        }
                    }
                }
            }
            case("reset") {
                actionFuture { future ->
                    when (val parm = script().getParameter()) {
                        is SkillParameter -> {
                            future.complete(SkillTimer.reset(script().sender!!, parm) / 50L)
                        }

                        is StationParameter<*> -> {
                            future.complete(StationTimer.reset(script().sender!!, parm) / 50L)
                        }

                        else -> null
                    }
                }
            }
            case("get/countdown") { getCountDown(it) }
            other { getCountDown(it) }
        }
    }

    private fun getCountDown(reader: QuestReader): ScriptAction<Any?> {
        val tag = reader.nextHeadActionOrNull("key")
        val type = reader.nextHeadActionOrNull("type")

        return object : CooldownScriptAction(tag, type) {

            override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                future.complete(SkillTimer.getCountdown(script().sender!!, skill))
            }

            override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                future.complete(StationTimer.getCountdown(script().sender!!, station))
            }
        }
    }

    enum class CooldownType {
        SKILL,
        STATION
    }

    abstract class CooldownScriptAction(val tag: ParsedAction<*>?, val type: ParsedAction<*>?): ScriptAction<Any?>() {

        override fun run(frame: ScriptFrame): CompletableFuture<Any?> {
            val future = CompletableFuture<Any?>()
            val type = type ?: literalAction(
                when (frame.script().getParameter()) {
                    is SkillParameter -> CooldownType.SKILL
                    is StationParameter<*> -> CooldownType.STATION
                    else -> "skill"
                }
            )
            frame.run(type).str { type ->
                when (type.uppercase()) {
                    CooldownType.SKILL.name -> {
                        frame.run(tag ?: literalAction((frame.script().getParameter() as SkillParameter).skill!!)).str { skill ->
                            frame.skill(future, skill)
                        }
                    }
                    CooldownType.STATION.name -> {
                        frame.run(tag ?: literalAction((frame.script().getParameter() as StationParameter<*>).stationLoader)).str { station ->
                            frame.station(future, station)
                        }
                    }
                }
            }
            return future
        }

        abstract fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String)

        abstract fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String)
    }
}
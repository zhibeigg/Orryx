package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.common.timer.StationTimer
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.nextHeadActionOrNull
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object CooldownActions {

    @KetherParser(["cooldown"], namespace = ORRYX_NAMESPACE)
    private fun actionCooldown() = scriptParser(
        arrayOf(
            Action.new("Cooldown冷却", "检测冷却", "cooldown")
                .description("检测技能是否在冷却中")
                .addEntry("检测标识符", type = Type.SYMBOL, head = "has")
                .addEntry("检测的技能", Type.STRING, true, "当前", "skill")
                .result("是否在冷却中", Type.BOOLEAN),
            Action.new("Cooldown冷却", "给予冷却", "cooldown")
                .description("给予玩家技能冷却")
                .addEntry("给予标识符", type = Type.SYMBOL, head = "add")
                .addEntry("冷却值", type = Type.LONG)
                .addEntry("技能", Type.STRING, true, "当前", "skill"),
            Action.new("Cooldown冷却", "减少冷却", "cooldown")
                .description("减少玩家技能冷却")
                .addEntry("减少标识符", type = Type.SYMBOL, head = "take")
                .addEntry("冷却值", type = Type.LONG)
                .addEntry("技能", Type.STRING, true, "当前", "skill"),
            Action.new("Cooldown冷却", "设置冷却", "cooldown")
                .description("设置玩家技能冷却")
                .addEntry("设置标识符", type = Type.SYMBOL, head = "set/to")
                .addEntry("冷却值", type = Type.LONG)
                .addEntry("技能", Type.STRING, true, "当前", "skill"),
            Action.new("Cooldown冷却", "重置冷却", "cooldown")
                .description("重置玩家技能冷却")
                .addEntry("重置标识符", type = Type.SYMBOL, head = "reset")
                .result("新的冷却值", Type.LONG),
            Action.new("Cooldown冷却", "获取倒计时", "cooldown")
                .description("获取玩家技能冷却倒计时")
                .addEntry("获取标识符", optional = true, type = Type.SYMBOL, head = "get/countdown")
                .addEntry("技能", Type.STRING, true, "当前", "skill")
                .result("倒计时", Type.LONG),
        )
    ) {
        it.switch {
            case("has") {
                val skill = it.nextHeadActionOrNull("skill")
                actionFuture { future ->
                    when(val parm = script().getParameter()) {
                        is SkillParameter -> {
                            run(skill ?: literalAction(parm.skill!!)).str { skill ->
                                future.complete(SkillTimer.hasNext(script().sender!!, skill))
                            }
                        }
                        is StationParameter<*> -> {
                            run(skill ?: literalAction(parm.stationLoader)).str { station ->
                                future.complete(StationTimer.hasNext(script().sender!!, station))
                            }
                        }
                        else -> future.complete(false)
                    }
                }
            }
            case("add") {
                val cooldown = it.nextParsedAction()
                val skill = it.nextHeadActionOrNull("skill")
                actionNow {
                    run(cooldown).long { cooldown ->
                        when(val parm = script().getParameter()) {
                            is SkillParameter -> {
                                run(skill ?: literalAction(parm.skill!!)).str { skill ->
                                    SkillTimer.increase(script().sender!!, skill, cooldown * 50)
                                }
                            }
                            is StationParameter<*> -> {
                                run(skill ?: literalAction(parm.stationLoader)).str { station ->
                                    StationTimer.increase(script().sender!!, station, cooldown * 50)
                                }
                            }
                            else -> null
                        }
                    }
                }
            }
            case("take") {
                val cooldown = it.nextParsedAction()
                val skill = it.nextHeadActionOrNull("skill")
                actionNow {
                    run(cooldown).long { cooldown ->
                        when(val parm = script().getParameter()) {
                            is SkillParameter -> {
                                run(skill ?: literalAction(parm.skill!!)).str { skill ->
                                    SkillTimer.reduce(script().sender!!, skill, cooldown * 50)
                                }
                            }
                            is StationParameter<*> -> {
                                run(skill ?: literalAction(parm.stationLoader)).str { station ->
                                    StationTimer.reduce(script().sender!!, station, cooldown * 50)
                                }
                            }
                            else -> null
                        }
                    }
                }
            }
            case("set", "to") {
                val cooldown = it.nextParsedAction()
                val skill = it.nextHeadActionOrNull("skill")
                actionNow {
                    run(cooldown).long { cooldown ->
                        when(val parm = script().getParameter()) {
                            is SkillParameter -> {
                                run(skill ?: literalAction(parm.skill!!)).str { skill ->
                                    SkillTimer.set(script().sender!!, skill, cooldown * 50)
                                }
                            }
                            is StationParameter<*> -> {
                                run(skill ?: literalAction(parm.stationLoader)).str { station ->
                                    StationTimer.set(script().sender!!, station, cooldown * 50)
                                }
                            }
                            else -> null
                        }
                    }
                }
            }
            case("reset") {
                actionFuture { future ->
                    when (val parm = script().getParameter()) {
                        is SkillParameter -> {
                            future.complete(SkillTimer.reset(script().sender!!, parm)/50L)
                        }

                        is StationParameter<*> -> {
                            future.complete(StationTimer.reset(script().sender!!, parm)/50L)
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
        val skill = reader.nextHeadActionOrNull("skill")
        return actionFuture { future ->
            when (val parm = script().getParameter()) {
                is SkillParameter -> {
                    run(skill ?: literalAction(parm.skill!!)).str { skill ->
                        future.complete(SkillTimer.getCountdown(script().sender!!, skill))
                    }
                }

                is StationParameter<*> -> {
                    run(skill ?: literalAction(parm.stationLoader)).str { station ->
                        future.complete(StationTimer.getCountdown(script().sender!!, station))
                    }
                }

                else -> future.complete(0)
            }
        }
    }

}
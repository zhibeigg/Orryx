package org.gitee.orryx.core.kether.actions

import eos.moe.dragoncore.network.PacketSender
import org.bukkit.entity.Player
import org.gitee.orryx.core.skill.PressSkillManager
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.container
import org.gitee.orryx.utils.firstInstanceOrNull
import org.gitee.orryx.utils.forEachInstance
import org.gitee.orryx.utils.nextHeadActionOrNull
import org.gitee.orryx.utils.nextTheyContainerOrNull
import org.gitee.orryx.utils.scriptParser
import org.gitee.orryx.utils.self
import taboolib.common5.clong
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*

object PressSkillActions {

    @KetherParser(["press"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionPress() = scriptParser(
        Action.new("PressSkill蓄力技能", "获取玩家正在蓄力的技能", "press", true)
            .description("获取玩家正在蓄力的技能")
            .addEntry("获取占位符", Type.SYMBOL, false, head = "get")
            .addContainerEntry("目标", true, "@self")
            .result("玩家正在蓄力的技能", Type.STRING),
        Action.new("PressSkill蓄力技能", "发送蓄力进度条", "press", true)
            .description("发送蓄力进度条")
            .addEntry("发送占位符", Type.SYMBOL, false, head = "send")
            .addEntry("最大蓄力时间", Type.LONG, false)
            .addEntry("蓄力阶段时间", Type.ITERABLE, true, head = "progress")
            .addContainerEntry("目标", true, "@self")
            .example("press bar send 60 progress 10,20,40 they @self"),
        Action.new("PressSkill蓄力技能", "清除蓄力进度条", "press", true)
            .description("清除蓄力进度条")
            .addEntry("清除占位符", Type.SYMBOL, false, head = "clear")
            .addContainerEntry("目标", true, "@self")
            .example("press bar clear they @self")
    ) {
        it.switch {
            case("get") { getPressSkill(it) }
            case("bar") {
                it.switch {
                    case("send") {
                        sendPressSkillBar(it)
                    }
                    case("clear") {
                        clearPressSkillBar(it)
                    }
                }
            }
        }
    }

    fun getPressSkill(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()
        return actionFuture { future ->
            container(they, self()) {
                val player = it.firstInstanceOrNull<PlayerTarget>()
                if (player != null) {
                    future.complete(PressSkillManager.pressTaskMap[player.uniqueId]?.first)
                } else {
                    future.complete(null)
                }
            }
        }
    }

    fun sendPressSkillBar(reader: QuestReader): ScriptAction<Any?> {
        val max = reader.nextParsedAction()
        val ticks = reader.nextHeadActionOrNull("progress")
        val they = reader.nextTheyContainerOrNull()
        return actionNow {
            run(max).long { max ->
                if (ticks != null) {
                    run(ticks).str { ticks ->
                        container(they, self()) {
                            val ticks = ticks.split(",").map { v -> v.clong }.toLongArray()
                            it.forEachInstance<PlayerTarget> { player ->
                                sendPressProgressBar(player.getSource(), max, *ticks)
                            }
                        }
                    }
                } else {
                    container(they, self()) {
                        it.forEachInstance<PlayerTarget> { player ->
                            sendPressProgressBar(player.getSource(), max)
                        }
                    }
                }
            }
        }
    }

    fun sendPressProgressBar(player: Player, maxTick: Long, vararg progressTicks: Long) {
        PacketSender.sendSyncPlaceholder(player, mapOf(
            "orryx_press_bar" to "$maxTick:${progressTicks.joinToString(":")}"
        ))
    }

    fun clearPressSkillBar(reader: QuestReader): ScriptAction<Any?> {
        val they = reader.nextTheyContainerOrNull()
        return actionNow {
            container(they, self()) {
                it.forEachInstance<PlayerTarget> { player ->
                    PacketSender.sendDeletePlaceholderCache(player.getSource(), "orryx_press_bar", false)
                }
            }
        }
    }
}
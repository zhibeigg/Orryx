package org.gitee.orryx.core.kether.actions

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.timer.CooldownEntry
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.common.timer.StationTimer
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Property
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.completeInto
import org.gitee.orryx.utils.getParameter
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.mainThreadFuture
import org.gitee.orryx.utils.nextHeadActionOrNull
import org.gitee.orryx.utils.registerProperty
import org.gitee.orryx.utils.scriptParser
import org.gitee.orryx.utils.ticksToMillisSaturated
import taboolib.common.OpenResult
import taboolib.library.kether.ParsedAction
import taboolib.library.kether.QuestReader
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object CooldownActions {

    init {
        registerProperty(
            cooldownEntryProperty(),
            Property.new("Cooldown冷却", "CooldownEntry", "orryx.cooldown.entry.operator")
                .description("冷却条目对象，包含冷却相关信息")
                .addEntry("tag", Type.STRING, "冷却标签")
                .addEntry("countdown", Type.LONG, "剩余冷却时间(ms)")
                .addEntry("overStamp", Type.LONG, "冷却结束时间戳")
                .addEntry("isReady", Type.BOOLEAN, "是否已就绪"),
            CooldownEntry::class.java
        )
    }

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
                        mainThreadFuture { !SkillTimer.hasNext(script().sender!!, skill) }.completeInto(future)
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        mainThreadFuture { !StationTimer.hasNext(script().sender!!, station) }.completeInto(future)
                    }
                }
            }
            case("add") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            SkillTimer.increaseAsync(script().sender!!, skill, ticksToMillisSaturated(ticks))
                        }.completeInto(future)
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            mainThreadFuture {
                                StationTimer.increase(script().sender!!, station, ticksToMillisSaturated(ticks))
                            }
                        }.completeInto(future)
                    }
                }
            }
            case("take") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            SkillTimer.reduceAsync(script().sender!!, skill, ticksToMillisSaturated(ticks))
                        }.completeInto(future)
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            mainThreadFuture {
                                StationTimer.reduce(script().sender!!, station, ticksToMillisSaturated(ticks))
                            }
                        }.completeInto(future)
                    }
                }
            }
            case("set", "to") {
                val cooldown = it.nextParsedAction()
                val tag = it.nextHeadActionOrNull("key")
                val type = it.nextHeadActionOrNull("type")

                object : CooldownScriptAction(tag, type) {

                    override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            SkillTimer.setAsync(script().sender!!, skill, ticksToMillisSaturated(ticks))
                        }.completeInto(future)
                    }

                    override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                        run(cooldown).long { it }.thenCompose { ticks ->
                            mainThreadFuture {
                                StationTimer.set(script().sender!!, station, ticksToMillisSaturated(ticks))
                            }
                        }.completeInto(future)
                    }
                }
            }
            case("reset") {
                actionFuture { future ->
                    val stage = when (val parm = script().getParameter()) {
                        is SkillParameter -> {
                            val player = script().sender!!.castSafely<Player>()
                            val skillKey = parm.skill
                            if (player == null || skillKey == null) {
                                failedFuture<Long>(IllegalArgumentException("技能冷却重置需要有效玩家与技能"))
                            } else {
                                player.getSkill(skillKey).thenCompose { skill ->
                                    skill?.let { SkillTimer.resetAsync(it, parm) }
                                        ?: failedFuture(IllegalArgumentException("未找到玩家技能 $skillKey"))
                                }.thenApply { it / 50L }
                            }
                        }

                        is StationParameter<*> -> mainThreadFuture {
                            StationTimer.reset(script().sender!!, parm) / 50L
                        }

                        else -> failedFuture(IllegalArgumentException("当前脚本环境没有技能或中转站参数"))
                    }
                    stage.completeInto(future)
                }
            }
            case("get", "countdown") { getCountDown(it) }
            other { getCountDown(it) }
        }
    }

    private fun getCountDown(reader: QuestReader): ScriptAction<Any?> {
        val tag = reader.nextHeadActionOrNull("key")
        val type = reader.nextHeadActionOrNull("type")

        return object : CooldownScriptAction(tag, type) {

            override fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String) {
                mainThreadFuture { SkillTimer.getCountdown(script().sender!!, skill) }.completeInto(future)
            }

            override fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String) {
                mainThreadFuture { StationTimer.getCountdown(script().sender!!, station) }.completeInto(future)
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
            val typeAction = type ?: literalAction(
                when (frame.script().getParameter()) {
                    is SkillParameter -> CooldownType.SKILL
                    is StationParameter<*> -> CooldownType.STATION
                    else -> CooldownType.SKILL
                }
            )
            frame.run(typeAction).str { it }.thenCompose { resolvedType ->
                when (resolvedType.uppercase(java.util.Locale.ROOT)) {
                    CooldownType.SKILL.name -> {
                        val tagAction = tag ?: (frame.script().getParameter() as? SkillParameter)?.skill?.let(::literalAction)
                            ?: return@thenCompose failedFuture<Unit>(IllegalArgumentException("缺少技能冷却 key"))
                        frame.run(tagAction).str { it }.thenApply { skill ->
                            frame.skill(future, skill)
                            Unit
                        }
                    }

                    CooldownType.STATION.name -> {
                        val tagAction = tag ?: (frame.script().getParameter() as? StationParameter<*>)?.stationLoader?.let(::literalAction)
                            ?: return@thenCompose failedFuture<Unit>(IllegalArgumentException("缺少中转站冷却 key"))
                        frame.run(tagAction).str { it }.thenApply { station ->
                            frame.station(future, station)
                            Unit
                        }
                    }

                    else -> failedFuture(IllegalArgumentException("未知冷却类型: $resolvedType"))
                }
            }.whenComplete { _, throwable ->
                if (throwable != null) future.completeExceptionally(throwable)
            }
            return future
        }

        abstract fun ScriptFrame.skill(future: CompletableFuture<Any?>, skill: String)

        abstract fun ScriptFrame.station(future: CompletableFuture<Any?>, station: String)
    }

    private fun <T> failedFuture(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    private fun cooldownEntryProperty() = object : ScriptProperty<CooldownEntry>("orryx.cooldown.entry.operator") {

        override fun read(instance: CooldownEntry, key: String): OpenResult {
            return when (key) {
                "tag" -> OpenResult.successful(instance.tag)
                "countdown" -> OpenResult.successful(instance.countdown)
                "overStamp" -> OpenResult.successful(instance.overStamp)
                "isReady" -> OpenResult.successful(instance.isReady)
                else -> OpenResult.failed()
            }
        }

        override fun write(instance: CooldownEntry, key: String, value: Any?): OpenResult {
            return OpenResult.failed()
        }
    }
}
package org.gitee.orryx.core.kether.actions.game

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.interfaces.ITimedStatus
import org.gitee.orryx.core.targets.PlayerTarget
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.*
import taboolib.library.kether.ParsedAction
import taboolib.module.kether.*
import java.util.concurrent.CompletableFuture

object PlayerProfileActions {

    @KetherParser(["superBody"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superBody() = timedStatusParser("霸体", "superBody") { Orryx.api().profileAPI.superBody() }

    @KetherParser(["superFoot"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun superFoot() = timedStatusParser("免疫摔落", "superFoot") { Orryx.api().profileAPI.superFoot() }

    @KetherParser(["invincible"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun invincible() = timedStatusParser("无敌", "invincible") { Orryx.api().profileAPI.invincible() }

    @KetherParser(["silence"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun silence() = timedStatusParser("沉默", "silence") { Orryx.api().profileAPI.silence() }

    private fun timedStatusParser(
        displayName: String,
        command: String,
        status: () -> ITimedStatus,
    ): ScriptActionParser<Any?> {
        val parser = scriptParser(
            Action.new("Orryx Profile玩家信息", "设置${displayName}状态", command, true)
                .description("设置、增加、减少或取消${displayName}状态")
                .addEntry("设置方法", Type.SYMBOL, false, head = "set/to/=,add/+,reduce/-,cancel/stop")
                .addEntry("状态时长", Type.LONG, true, default = "0")
                .addContainerEntry(optional = true, default = "@self"),
            Action.new("Orryx Profile玩家信息", "获取${displayName}状态倒计时", command, true)
                .description("获取${displayName}状态倒计时")
                .addEntry("倒计时占位符", Type.SYMBOL, false, head = "count")
                .addContainerEntry(optional = true, default = "@self")
                .result("倒计时Tick", Type.LONG),
        ) { reader ->
            reader.switch {
                case("set", "to", "=") {
                    timedMutation(nextParsedAction(), nextTheyContainerOrNull(), status, TimedOperation.SET)
                }
                case("add", "+") {
                    timedMutation(nextParsedAction(), nextTheyContainerOrNull(), status, TimedOperation.ADD)
                }
                case("reduce", "-") {
                    timedMutation(nextParsedAction(), nextTheyContainerOrNull(), status, TimedOperation.REDUCE)
                }
                case("cancel", "stop") {
                    timedCancel(nextTheyContainerOrNull(), status)
                }
                case("count") {
                    timedCountdown(nextTheyContainerOrNull(), status)
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return parser as ScriptActionParser<Any?>
    }

    private fun timedMutation(
        timeout: ParsedAction<*>,
        targets: ParsedAction<*>?,
        status: () -> ITimedStatus,
        operation: TimedOperation,
    ): ScriptAction<Any?> {
        return actionFuture { future ->
            run(timeout).long { it }.thenCompose { ticks ->
                players(targets).thenCompose { players ->
                    mainThreadFuture {
                        val millis = ticksToMillisSaturated(ticks)
                        val timedStatus = status()
                        players.forEach { player ->
                            when (operation) {
                                TimedOperation.SET -> timedStatus.set(player, millis)
                                TimedOperation.ADD -> timedStatus.add(player, millis)
                                TimedOperation.REDUCE -> timedStatus.reduce(player, millis)
                            }
                        }
                        Unit
                    }
                }
            }.completeInto(future)
        }
    }

    private fun timedCancel(
        targets: ParsedAction<*>?,
        status: () -> ITimedStatus,
    ): ScriptAction<Any?> {
        return actionFuture { future ->
            players(targets).thenCompose { players ->
                mainThreadFuture {
                    val timedStatus = status()
                    players.forEach(timedStatus::cancel)
                    Unit
                }
            }.completeInto(future)
        }
    }

    private fun timedCountdown(
        targets: ParsedAction<*>?,
        status: () -> ITimedStatus,
    ): ScriptAction<Any?> {
        return actionFuture { future ->
            players(targets).thenCompose { players ->
                mainThreadFuture {
                    players.firstOrNull()?.let { status().countdown(it) / 50L } ?: 0L
                }
            }.completeInto(future)
        }
    }

    private fun ScriptFrame.players(targets: ParsedAction<*>?): CompletableFuture<List<Player>> {
        return containerOrSelf(targets) { container ->
            container.mapInstance<PlayerTarget, Player> { it.getSource() }
        }
    }

    private enum class TimedOperation {
        SET,
        ADD,
        REDUCE,
    }
}

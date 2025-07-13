package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.module.state.Status
import org.gitee.orryx.module.state.states.BlockState
import org.gitee.orryx.module.state.states.DodgeState
import org.gitee.orryx.module.state.states.GeneralAttackState
import org.gitee.orryx.module.state.states.PressGeneralAttackState
import org.gitee.orryx.module.state.states.VertigoState
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.ensureSync
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.*

object StateActions {

    @KetherParser(["running"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionRunning() = combinationParser(
        Action.new("State状态机", "获取可运行state", "running", true)
            .description("获取可运行state")
            .addEntry("状态名", Type.STRING, false)
            .result("运动状态", Type.STATE)
    ) {
        it.group(
            symbol()
        ).apply(it) { state ->
            future {
                ensureSync {
                    val data = script().bukkitPlayer().statusData()
                    val status = data.status as? Status ?: return@ensureSync null
                    val actionState = status.privateStates[state] ?: StateManager.getGlobalState(state)
                    when (actionState) {
                        is BlockState -> BlockState.Running(data, actionState)
                        is DodgeState -> DodgeState.Running(data, actionState)
                        is GeneralAttackState -> GeneralAttackState.Running(data, actionState)
                        is PressGeneralAttackState -> PressGeneralAttackState.Running(data, actionState)
                        is VertigoState -> VertigoState.Running(data, actionState)
                        else -> null
                    }
                }
            }
        }
    }

    @KetherParser(["state"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionState() = scriptParser(
        Action.new("State状态机", "获取当前运行的状态名", "state", true)
            .description("获取当前运行的状态名")
            .addEntry("当前占位符", Type.SYMBOL, false, head = "now")
            .result("状态名", Type.STRING),
        Action.new("State状态机", "获取当前移动方向", "state", true)
            .description("获取当前移动方向")
            .addEntry("移动占位符", Type.SYMBOL, false, head = "move")
            .result("移动方向", Type.STRING),
        Action.new("State状态机", "自动检测Status条件更新", "state", true)
            .description("自动检测Status条件并更新Status")
            .addEntry("更新占位符", Type.SYMBOL, false, head = "update")
            .result("适配的Status名", Type.STRING),
        Action.new("State状态机", "强制执行下一状态", "state", true)
            .description("强制执行指定下一状态")
            .addEntry("下一占位符", Type.SYMBOL, false, head = "next")
            .addEntry("状态名", Type.STRING)
            .result("运动状态", Type.STATE)
    ) {
        it.switch {
            case("now") {
                actionNow {
                    script().bukkitPlayer().statusData().nowRunningState?.state?.key
                }
            }
            case("move") {
                actionNow {
                    script().bukkitPlayer().statusData().moveState.name
                }
            }
            case("update") {
                actionTake {
                    StateManager.autoCheckStatus(script().bukkitPlayer()).thenApply { status ->
                        status?.key
                    }
                }
            }
            case("next") {
                val state = nextParsedAction()
                actionFuture { future ->
                    run(state).str { state ->
                        ensureSync {
                            val data = script().bukkitPlayer().statusData()
                            val status = data.status as? Status ?: return@ensureSync
                            val actionState = status.privateStates[state] ?: StateManager.getGlobalState(state)
                            val running = when (actionState) {
                                is BlockState -> BlockState.Running(data, actionState)
                                is DodgeState -> DodgeState.Running(data, actionState)
                                is GeneralAttackState -> GeneralAttackState.Running(data, actionState)
                                is PressGeneralAttackState -> PressGeneralAttackState.Running(data, actionState)
                                is VertigoState -> VertigoState.Running(data, actionState)
                                else -> null
                            }
                            running?.let { it1 -> data.next(it1) }
                            future.complete(running)
                        }
                    }
                }
            }
        }
    }
}
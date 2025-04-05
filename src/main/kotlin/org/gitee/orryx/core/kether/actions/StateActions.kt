package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.combinationParser
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.StateManager.statusData
import org.gitee.orryx.module.state.Status
import org.gitee.orryx.module.state.states.BlockState
import org.gitee.orryx.module.state.states.DodgeState
import org.gitee.orryx.module.state.states.GeneralAttackState
import org.gitee.orryx.module.state.states.VertigoState
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.bukkitPlayer
import org.gitee.orryx.utils.ensureSync
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionNow
import taboolib.module.kether.script
import taboolib.module.kether.switch

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
                    val status = script().get<Status>("status")
                    val actionState = status?.privateStates?.get(state) ?: StateManager.getGlobalState(state)
                    when (actionState) {
                        is BlockState -> BlockState.Running(data, actionState)
                        is DodgeState -> DodgeState.Running(data, actionState)
                        is GeneralAttackState -> GeneralAttackState.Running(data, actionState)
                        is VertigoState -> VertigoState.Running(data, actionState)
                        else -> null
                    }
                }
            }
        }
    }

    @KetherParser(["state"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionState() = scriptParser(
        arrayOf(
            Action.new("State状态机", "获取当前运行的状态名", "state", true)
                .description("获取当前运行的状态名")
                .addEntry("当前占位符", Type.SYMBOL, false, head = "now")
                .result("状态名", Type.STRING),
            Action.new("State状态机", "获取当前移动方向", "state", true)
                .description("获取当前移动方向")
                .addEntry("当前占位符", Type.SYMBOL, false, head = "move")
                .result("移动方向", Type.STRING)
        )
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
        }
    }

}
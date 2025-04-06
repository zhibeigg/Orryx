package org.gitee.orryx.module.state

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.module.state.states.BlockState
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerData(val player: Player) {

    var status: IStatus? = null
        private set

    private var changeMoveState = MoveState.FRONT

    //移动方向
    val moveState: MoveState
        get() = MoveState.entries.firstOrNull {
            KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeyPress(it.key) == true
        } ?: changeMoveState

    //预输入
    var nextInput: String? = null

    //现在的运行状态
    var nowRunningState: IRunningState? = null
        private set

    /**
     * 尝试执行下一个状态
     * @param input 用于读取下一操作的输入键
     * @return 过渡到的状态
     * */
    fun tryNext(input: String): CompletableFuture<IRunningState?>? {
        return status?.next(this, input)?.thenApply {
            if (it == null) {
                nextInput = input
                return@thenApply null
            }
            if (nowRunningState == null || nowRunningState!!.hasNext(it)) {
                nextInput = null
                nowRunningState = it
                it.start()
                it.state.script?.also { script -> runScript(script) }
                it
            } else {
                nextInput = input
                null
            }
        }
    }

    /**
     * 强制执行下一个状态
     * @param running 用于读取下一操作的输入键
     * @return 过渡到的状态
     * */
    fun next(running: IRunningState) {
        nowRunningState?.stop()
        nextInput = null
        nowRunningState = running
        running.start()
        running.state.script?.also { script -> runScript(script) }
    }

    fun clearRunningState() {
        nowRunningState = null
    }

    private fun runScript(script: Script) {
        ScriptContext.create(script).also {
            it.sender = adaptPlayer(player)
            it.id = UUID.randomUUID().toString()
        }.runActions()
    }

    fun setStatus(status: Status?) {
        this.status = status
    }

    fun updateMoveState(input: String) {
        when(input) {
            MoveState.FRONT.key -> changeMoveState = MoveState.FRONT
            MoveState.REAR.key -> changeMoveState = MoveState.REAR
            MoveState.LEFT.key -> changeMoveState = MoveState.LEFT
            MoveState.RIGHT.key -> changeMoveState = MoveState.RIGHT
        }
    }

    fun blockSuccess() {
        val state = nowRunningState as? BlockState.Running ?: return
        state.success()
    }

}
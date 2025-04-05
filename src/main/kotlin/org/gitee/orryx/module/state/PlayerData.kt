package org.gitee.orryx.module.state

import org.bukkit.entity.Player
import org.gitee.orryx.module.state.states.BlockState
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerData(val player: Player) {

    var status: IStatus? = null
        private set

    //移动方向
    var moveState: MoveState = MoveState.FRONT

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
    fun next(input: String): CompletableFuture<IRunningState?>? {
        return status?.next(this, input)?.thenApply {
            if (it == null || nowRunningState == null) {
                nextInput = input
                return@thenApply null
            }
            if (nowRunningState!!.hasNext(it)) {
                nowRunningState = it
                it.start()
                it.state.script?.also { runScript(it, input) }
                it
            } else {
                nextInput = input
                null
            }
        }
    }

    private fun runScript(script: Script, input: String) {
        ScriptContext.create(script).also {
            it.sender = adaptPlayer(player)
            it.id = UUID.randomUUID().toString()
            it["input"] = input
        }.runActions()
    }

    fun setStatus(status: Status) {
        this.status = status
    }

    fun blockSuccess() {
        val state = nowRunningState as? BlockState.Running ?: return
        state.success()
    }

}
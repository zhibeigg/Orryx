package org.gitee.orryx.module.state

import org.bukkit.entity.Player
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.module.state.StateManager.getController
import org.gitee.orryx.module.state.StateManager.statusDataList
import org.gitee.orryx.module.state.states.BlockState
import java.util.*
import java.util.concurrent.CompletableFuture

class PlayerData(val player: Player) {

    var status: IStatus? = null
        private set

    private var changeMoveState = MoveState.FRONT

    val cacheJoiner = hashSetOf<UUID>()

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
    }

    fun clearRunningState() {
        nowRunningState = null
    }

    fun setStatus(status: Status?) {
        this.status = status
        if (status != null) {
            val controller = getController(status.options.controller) ?: return
            DragonCoreCustomPacketSender.setPlayerAnimationController(player, player.uniqueId, controller.saveToString())
            statusDataList().forEach {
                if (player.uniqueId in it.cacheJoiner) {
                    DragonCoreCustomPacketSender.setPlayerAnimationController(it.player, player.uniqueId, controller.saveToString())
                }
            }
        } else {
            DragonCoreCustomPacketSender.removePlayerAnimationController(player, player.uniqueId)
            statusDataList().forEach {
                if (player.uniqueId in it.cacheJoiner) {
                    DragonCoreCustomPacketSender.removePlayerAnimationController(it.player, player.uniqueId)
                }
            }
        }
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
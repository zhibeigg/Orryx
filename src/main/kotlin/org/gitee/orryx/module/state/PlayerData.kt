package org.gitee.orryx.module.state

import org.bukkit.entity.Player
import java.util.concurrent.locks.ReentrantLock

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
    fun next(input: String): IRunningState? {

    }

}
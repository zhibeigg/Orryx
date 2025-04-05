package org.gitee.orryx.module.state

import java.util.concurrent.CompletableFuture

interface IStatus {

    /**
     * 状态机配置键名
     * */
    val key: String

    /**
     * 获取下一个状态
     * @param playerData 玩家状态机信息
     * @param input 输入按键
     * @return [IRunningState]获取到的下一个状态
     * */
    fun next(playerData: PlayerData, input: String): CompletableFuture<IRunningState?>

}
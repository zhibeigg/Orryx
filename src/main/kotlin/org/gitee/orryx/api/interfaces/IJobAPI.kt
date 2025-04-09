package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import java.util.concurrent.CompletableFuture

interface IJobAPI {

    /**
     * 获取玩家Job并进行操作
     *
     * @param player 玩家
     * @param job 职业(默认当前职业)
     * @param function 获取到职业后执行
     * @return [function]的返回值
     * */
    fun <T> modifyJob(player: Player, job: String? = null, function: (job: IPlayerJob) -> T) : CompletableFuture<T?>

    fun getJob(job: String): IJob?

}
package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.IPlayerJob
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * 职业 API 接口
 *
 * 提供对玩家职业数据的访问和修改功能
 * */
interface IJobAPI {

    /**
     * 获取玩家职业并进行操作
     *
     * 注意：此方法不会自动保存更改，如需持久化请在修改后手动调用 [IPlayerJob.save] 方法
     *
     * @param player 玩家
     * @param job 职业键名，为 null 时使用玩家当前职业
     * @param function 获取到职业后执行的函数
     * @return [function] 的返回值，如果玩家没有该职业则返回 null
     * */
    fun <T> modifyJob(player: Player, job: String? = null, function: Function<IPlayerJob, T>) : CompletableFuture<T?>

    /**
     * 获取职业配置
     *
     * @param job 职业键名
     * @return 职业配置对象，如果不存在则返回 null
     * */
    fun getJob(job: String): IJob?
}
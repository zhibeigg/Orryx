package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob

interface IPlayerProfile {

    /**
     * 玩家
     * */
    val player: Player

    /**
     * 玩家当前职业
     * */
    val job: String?

    /**
     * 玩家技能点
     * */
    val point: Int

    /**
     * 玩家的FLAG存储器
     * */
    val flags: Map<String, IFlag<*>>

    /**
     * 尝试获取flag
     * @param flagName flag的键名
     * @return flag
     * */
    fun getFlag(flagName: String): IFlag<*>?

    /**
     * 设置flag
     * @param flagName flag的键名
     * @param flag flag
     * */
    fun setFlag(flagName: String, flag: IFlag<*>)

    /**
     * 是否在霸体状态
     * @return 是否在霸体状态
     */
    fun isSuperBody(): Boolean

    /**
     * 设置霸体时间
     * @param timeout 霸体时长
     */
    fun setSuperBody(timeout: Long)

    /**
     * 取消霸体
     */
    fun cancelSuperBody()

    /**
     * 延长霸体时间
     * @param timeout 延长的霸体时长
     */
    fun addSuperBody(timeout: Long)

    /**
     * 减少霸体时间
     * @param timeout 减少的霸体时长
     */
    fun reduceSuperBody(timeout: Long)

    /**
     * 给予玩家技能点
     * @param point 技能点
     */
    fun givePoint(point: Int)

    /**
     * 拿走玩家技能点
     * @param point 技能点
     */
    fun takePoint(point: Int)

    /**
     * 设置玩家技能点
     * @param point 技能点
     */
    fun setPoint(point: Int)

    /**
     * 设置玩家当前职业
     * @param job 玩家职业
     * */
    fun setJob(job: IPlayerJob)

    /**
     * 保存玩家数据
     * @param async 是否异步
     * */
    fun save(async: Boolean = false)

}
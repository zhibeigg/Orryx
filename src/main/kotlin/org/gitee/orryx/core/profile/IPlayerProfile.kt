package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.Saveable
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import java.util.UUID

interface IPlayerProfile: Saveable {

    /**
     * 玩家ID
     * */
    val id: Int

    /**
     * 玩家
     * */
    val uuid: UUID

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
    val flags: Map<String, IFlag>

    /**
     * 尝试获取flag
     * @param flagName flag的键名
     * @return flag
     * */
    fun getFlag(flagName: String): IFlag?

    /**
     * 设置flag
     * @param flagName flag的键名
     * @param flag flag
     * @param save 是否检测是否持久并保存
     * */
    fun setFlag(flagName: String, flag: IFlag, save: Boolean = true)

    /**
     * 移除flag
     * @param flagName flag的键名
     * @param save 是否检测是否持久并保存
     * @return 移除的flag
     * */
    fun removeFlag(flagName: String, save: Boolean = true): IFlag?

    /**
     * 清除flag
     * */
    fun clearFlags()

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
     * 创建序列化存储数据
     * */
    fun createPO(): PlayerProfilePO
}
package org.gitee.orryx.core.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.experience.IExperience

interface IPlayerJob {

    /**
     * 拥有玩家
     * */
    val player: Player

    /**
     * 职业键名
     * */
    val key: String

    /**
     * 职业配置文件
     * */
    val job: IJob

    /**
     * 职业当前选择的技能组
     *
     * 默认default
     * */
    val group: String

    /**
     * 等级(30/100 Lv.6)中的6
     * */
    val level: Int

    /**
     * 经验值(30/100 Lv.6)中的30加前六级经验
     * */
    val experience: Int

    /**
     * 当前等级下的经验值(30/100 Lv.6)中的30
     * */
    val experienceOfLevel: Int

    /**
     * 当前等级下的最大经验值(30/100 Lv.6)中的100
     * */
    val maxExperienceOfLevel: Int

    /**
     * 获得经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun giveExperience(experience: Int): ExperienceResult

    /**
     * 失去经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun takeExperience(experience: Int): ExperienceResult

    /**
     * 设置经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun setExperience(experience: Int): ExperienceResult

    /**
     * 获得等级
     * @param level 等级
     * @return 结果
     * */
    fun giveLevel(level: Int): LevelResult

    /**
     * 失去等级
     * @param level 等级
     * @return 结果
     * */
    fun takeLevel(level: Int): LevelResult

    /**
     * 设置等级
     * @param level 等级
     * @return 结果
     * */
    fun setLevel(level: Int): LevelResult

    /**
     * 获得经验计算器
     * @return 经验计算器
     * */
    fun getExperience(): IExperience

    /**
     * 获得最大法力值
     * @return 法力值
     * */
    fun getMaxMana(): Double

    /**
     * 获得恢复法力值
     * @return 法力值
     * */
    fun getReginMana(): Double

    /**
     * 设置技能组
     * @param group 已注册的技能组
     * @return 是否成功
     * */
    fun setGroup(group: String): Boolean

    /**
     * 保存数据
     * @param async 是否异步
     * */
    fun save(async: Boolean = true)

}
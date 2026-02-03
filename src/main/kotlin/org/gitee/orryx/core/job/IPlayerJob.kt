package org.gitee.orryx.core.job

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.dao.cache.Saveable
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.module.experience.IExperience
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 玩家职业数据接口。
 *
 * @property id 玩家 ID
 * @property uuid 玩家 UUID
 * @property player 玩家实体
 * @property key 职业键名
 * @property job 职业配置
 * @property bindKeyOfGroup 绑定的组与按键
 * @property group 当前选择的技能组，默认 default
 * @property level 当前等级
 * @property maxLevel 最大等级
 * @property experience 累计经验值
 * @property experienceOfLevel 当前等级经验值
 * @property maxExperienceOfLevel 当前等级最大经验值
 */
interface IPlayerJob: Saveable {

    val id: Int

    val uuid: UUID

    val player: Player

    val key: String

    val job: IJob

    val bindKeyOfGroup: Map<IGroup, Map<IBindKey, String?>>

    val group: String

    val level: Int

    val maxLevel: Int

    val experience: Int

    val experienceOfLevel: Int

    val maxExperienceOfLevel: Int

    /**
     * 获得经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun giveExperience(experience: Int): CompletableFuture<ExperienceResult>

    /**
     * 失去经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun takeExperience(experience: Int): CompletableFuture<ExperienceResult>

    /**
     * 设置经验值
     * @param experience 经验值
     * @return 结果
     * */
    fun setExperience(experience: Int): CompletableFuture<ExperienceResult>

    /**
     * 获得等级
     * @param level 等级
     * @return 结果
     * */
    fun giveLevel(level: Int): CompletableFuture<LevelResult>

    /**
     * 失去等级
     * @param level 等级
     * @return 结果
     * */
    fun takeLevel(level: Int): CompletableFuture<LevelResult>

    /**
     * 设置等级
     * @param level 等级
     * @return 结果
     * */
    fun setLevel(level: Int): CompletableFuture<LevelResult>

    /**
     * 获取升级会带来的技能点点数
     *
     * @param from 起始等级
     * @param to 目标等级
     * @return 获取的技能点数
     */
    fun getUpgradePoint(from: Int, to: Int): Int

    /**
     * 获取属性数据
     *
     * @return 属性数据列表
     */
    fun getAttributes(): List<String>

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
    fun getRegainMana(): Double

    /**
     * 获得最大精力值
     * @return 精力值
     * */
    fun getMaxSpirit(): Double

    /**
     * 获得恢复精力值
     * @return 精力值
     * */
    fun getRegainSpirit(): Double


    /**
     * 设置技能组
     * @param group 已注册的技能组
     * @return 是否成功
     * */
    fun setGroup(group: String): CompletableFuture<Boolean>

    /**
     * 设置技能绑定按键
     * @param skill 技能
     * @param group 技能组
     * @param bindKey 绑定按键
     * @return 是否成功
     * */
    fun setBindKey(skill: IPlayerSkill, group: IGroup, bindKey: IBindKey): CompletableFuture<Boolean>

    /**
     * 取消技能绑定按键
     * @param skill 技能
     * @param group 技能组
     * @return 是否成功
     * */
    fun unBindKey(skill: IPlayerSkill, group: IGroup): CompletableFuture<Boolean>

    /**
     * 清除此职业数据
     * @return 是否成功
     * */
    fun clear(): CompletableFuture<Boolean>

    /**
     * 创建序列化存储数据
     *
     * @return 序列化数据
     */
    fun createPO(): PlayerJobPO
}

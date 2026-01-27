package org.gitee.orryx.module.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.skill.SkillLevelResult
import java.util.concurrent.CompletableFuture

/**
 * 技能 UI 接口。
 *
 * @property viewer 查看 UI 的玩家
 * @property owner 拥有 UI 的玩家
 */
interface ISkillUI {

    val viewer: Player

    val owner: Player

    /**
     * 打开 UI。
     */
    fun open()

    /**
     * 更新 HUD。
     */
    fun update()

    /**
     * 设置技能绑定按键。
     *
     * @param job 职业
     * @param skill 技能键名
     * @param group 技能组键名
     * @param bindKey 绑定按键键名
     * @return 是否成功
     */
    fun bindSkill(job: IPlayerJob, skill: String, group: String, bindKey: String): CompletableFuture<Boolean>

    /**
     * 取消技能绑定。
     *
     * @param job 职业
     * @param skill 技能键名
     * @param group 技能组键名
     * @return 是否成功
     */
    fun unBindSkill(job: IPlayerJob, skill: String, group: String): CompletableFuture<Boolean>

    /**
     * 升级技能。
     *
     * @param skill 技能键名
     * @return 升级结果
     */
    fun upgrade(skill: String): CompletableFuture<SkillLevelResult>

    /**
     * 重置指定技能并返还技能点。
     *
     * @param skill 技能键名
     * @return 是否成功
     */
    fun clearAndBackPoint(skill: String): CompletableFuture<Boolean>

    /**
     * 重置所有技能并返还技能点。
     *
     * @return 是否成功
     */
    fun clearAllAndBackPoint(): CompletableFuture<Boolean>
}

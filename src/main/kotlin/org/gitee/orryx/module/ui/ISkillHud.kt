package org.gitee.orryx.module.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill
import java.util.concurrent.CompletableFuture

/**
 * 技能 HUD 接口。
 *
 * @property viewer 查看 HUD 的玩家
 * @property owner 拥有 HUD 的玩家
 */
interface ISkillHud {

    val viewer: Player

    val owner: Player

    /**
     * 打开 HUD。
     */
    fun open()

    /**
     * 更新 HUD。
     *
     * @param skill 需要更新的技能，null 表示整体刷新
     */
    fun update(skill: IPlayerSkill? = null)

    /**
     * 关闭 HUD。
     */
    fun close()

    /**
     * 设置使用的技能组。
     *
     * @param group 技能组
     * @return 是否设置成功
     */
    fun setGroup(group: IGroup): CompletableFuture<Boolean>

    /**
     * 获取展示的技能。
     *
     * @return 绑定按键对照技能
     */
    fun getShowSkills(): CompletableFuture<Map<IBindKey, String?>?>

    /**
     * 获取技能冷却倒计时。
     *
     * @param skill 技能
     * @return 倒计时（毫秒）
     */
    fun getCountdown(skill: IPlayerSkill): Long
}

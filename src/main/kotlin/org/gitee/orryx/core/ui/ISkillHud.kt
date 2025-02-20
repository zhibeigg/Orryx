package org.gitee.orryx.core.ui

import org.bukkit.entity.Player
import org.gitee.orryx.core.key.IBindKey
import org.gitee.orryx.core.key.IGroup
import org.gitee.orryx.core.skill.IPlayerSkill

interface ISkillHud {

    /**
     * 查看HUD的玩家
     * */
    val viewer: Player

    /**
     * 拥有HUD的玩家
     * */
    val owner: Player

    /**
     * 更新HUD
     * */
    fun update()

    /**
     * 设置使用的技能组
     * @param group 技能组
     * @return 是否设置成功
     * */
    fun setGroup(group: IGroup): Boolean

    /**
     * 获取展示的技能
     * @return 绑定按键对照技能
     * */
    fun getShowSkills(): Map<IBindKey, String?>

    /**
     * 获取技能冷却倒计时
     * @return 倒计时(毫秒)
     * */
    fun getCountdown(skill: IPlayerSkill): Long

}
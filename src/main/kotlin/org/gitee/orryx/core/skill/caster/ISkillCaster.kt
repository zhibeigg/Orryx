package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.ISkill

/**
 * 技能释放器接口。
 *
 * 定义技能释放的契约，不同类型的技能实现各自的释放逻辑。
 */
interface ISkillCaster {

    /**
     * 检查此释放器是否支持指定的技能。
     *
     * @param skill 要检查的技能
     * @return 如果支持则返回 true
     */
    fun supports(skill: ISkill): Boolean

    /**
     * 释放技能。
     *
     * @param skill 要释放的技能
     * @param player 释放技能的玩家
     * @param parameter 技能参数
     * @param consume 是否消耗资源（冷却、法力等）
     */
    fun cast(skill: ISkill, player: Player, parameter: SkillParameter, consume: Boolean)
}

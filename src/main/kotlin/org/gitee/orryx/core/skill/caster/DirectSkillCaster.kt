package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.DirectSkill
import org.gitee.orryx.utils.consume
import org.gitee.orryx.utils.runSkillAction

/**
 * 直接技能释放器。
 *
 * 处理 [DirectSkill] 类型的技能释放，立即执行技能脚本。
 */
object DirectSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean {
        return skill is DirectSkill
    }

    override fun cast(skill: ISkill, player: Player, parameter: SkillParameter, consume: Boolean) {
        skill as DirectSkill
        if (consume) skill.consume(player, parameter)
        parameter.runSkillAction()
    }
}

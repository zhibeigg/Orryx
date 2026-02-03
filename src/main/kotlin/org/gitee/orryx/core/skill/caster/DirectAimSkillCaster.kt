package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.DirectAimSkill
import org.gitee.orryx.utils.DEFAULT_PICTURE
import org.gitee.orryx.utils.consume
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.runSkillAction
import org.gitee.orryx.utils.toTarget
import taboolib.common5.cdouble
import taboolib.module.kether.orNull

/**
 * 直接指向性技能释放器。
 *
 * 处理 [DirectAimSkill] 类型的技能释放，请求客户端瞄准后执行技能。
 */
object DirectAimSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean {
        return skill is DirectAimSkill
    }

    override fun cast(skill: ISkill, player: Player, parameter: SkillParameter, consume: Boolean) {
        skill as DirectAimSkill
        val aimRadius = parameter.runCustomAction(skill.aimRadiusAction).orNull().cdouble
        val aimSize = parameter.runCustomAction(skill.aimSizeAction).orNull().cdouble
        PluginMessageHandler.requestAiming(player, skill.key, DEFAULT_PICTURE, aimSize, aimRadius) { aimInfo ->
            aimInfo.onSuccess {
                if (it.skillId == skill.key) {
                    parameter.origin = it.location.toTarget()
                    if (consume) skill.consume(player, parameter)
                    parameter.runSkillAction(
                        mapOf(
                            Pair("aimRadius", aimRadius),
                            Pair("aimSize", aimSize)
                        )
                    )
                }
            }
        }
    }
}

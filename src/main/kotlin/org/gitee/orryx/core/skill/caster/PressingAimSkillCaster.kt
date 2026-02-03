package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.PressingAimSkill
import org.gitee.orryx.utils.DEFAULT_PICTURE
import org.gitee.orryx.utils.consume
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.runSkillAction
import org.gitee.orryx.utils.toTarget
import taboolib.common5.cdouble
import taboolib.common5.clong
import taboolib.module.kether.orNull

/**
 * 蓄力指向性技能释放器。
 *
 * 处理 [PressingAimSkill] 类型的技能释放，结合蓄力和瞄准机制。
 */
object PressingAimSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean {
        return skill is PressingAimSkill
    }

    override fun cast(skill: ISkill, player: Player, parameter: SkillParameter, consume: Boolean) {
        skill as PressingAimSkill
        val aimRadius = parameter.runCustomAction(skill.aimRadiusAction).orNull().cdouble
        val aimMin = parameter.runCustomAction(skill.aimMinAction).orNull().cdouble
        val aimMax = parameter.runCustomAction(skill.aimMaxAction).orNull().cdouble
        val maxTick = parameter.runCustomAction(skill.maxPressTickAction).orNull().clong
        val timestamp = System.currentTimeMillis()

        PluginMessageHandler.requestAiming(
            player, skill.key, DEFAULT_PICTURE,
            aimMin, aimMax, aimRadius, maxTick
        ) { aimInfo ->
            aimInfo.getOrNull()?.let {
                if (it.skillId == skill.key) {
                    if (consume) skill.consume(player, parameter)
                    parameter.origin = it.location.toTarget()
                    parameter.runSkillAction(
                        mapOf(
                            Pair("aimRadius", aimRadius),
                            Pair("aimMin", aimMin),
                            Pair("aimMax", aimMax),
                            Pair("pressTick", (it.timestamp - timestamp) / 50)
                        )
                    )
                }
            }
        }
    }
}

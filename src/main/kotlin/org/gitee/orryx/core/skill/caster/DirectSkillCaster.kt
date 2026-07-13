package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.DirectSkill
import org.gitee.orryx.utils.consumeForStartup
import org.gitee.orryx.utils.finishConsumption
import org.gitee.orryx.utils.startSkillAction
import org.gitee.orryx.utils.thenComposeMain
import java.util.concurrent.CompletableFuture

/** 直接技能释放器。 */
object DirectSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean = skill is DirectSkill

    override fun cast(
        skill: ISkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
    ): CompletableFuture<CastResult> {
        skill as DirectSkill
        if (!consume) {
            return parameter.startSkillAction().thenApply { CastResult.SUCCESS }
        }
        return skill.consumeForStartup(player, parameter).thenComposeMain { consumption ->
            if (consumption.result == CastResult.SUCCESS) {
                parameter.finishConsumption(consumption) { parameter.startSkillAction() }
            } else {
                CompletableFuture.completedFuture(consumption.result)
            }
        }
    }
}

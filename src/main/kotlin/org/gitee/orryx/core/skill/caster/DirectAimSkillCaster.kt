package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.DirectAimSkill
import org.gitee.orryx.utils.DEFAULT_PICTURE
import org.gitee.orryx.utils.consume
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.startSkillAction
import org.gitee.orryx.utils.thenComposeMain
import org.gitee.orryx.utils.toTarget
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

/** 直接指向性技能释放器。 */
object DirectAimSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean = skill is DirectAimSkill

    override fun cast(
        skill: ISkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
    ): CompletableFuture<CastResult> {
        skill as DirectAimSkill
        val result = CompletableFuture<CastResult>()
        val aimRadius = parameter.runCustomAction(skill.aimRadiusAction).orNull().cdouble
        val aimSize = parameter.runCustomAction(skill.aimSizeAction).orNull().cdouble
        PluginMessageHandler.requestAiming(player, skill.key, DEFAULT_PICTURE, aimSize, aimRadius) { aimInfo ->
            aimInfo.onSuccess { info ->
                if (info.skillId != skill.key) {
                    result.complete(CastResult.CANCELED)
                    return@onSuccess
                }
                parameter.origin = info.location.toTarget()
                val consumption = if (consume) skill.consume(player, parameter) else CompletableFuture.completedFuture(CastResult.SUCCESS)
                consumption.thenComposeMain { castResult ->
                    if (castResult == CastResult.SUCCESS) {
                        parameter.startSkillAction(mapOf("aimRadius" to aimRadius, "aimSize" to aimSize))
                            .thenApply { castResult }
                    } else {
                        CompletableFuture.completedFuture(castResult)
                    }
                }.whenComplete { castResult, throwable ->
                    if (throwable == null) result.complete(castResult) else result.completeExceptionally(throwable)
                }
            }.onFailure { result.completeExceptionally(it) }
        }
        return result
    }
}

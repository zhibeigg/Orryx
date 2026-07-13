package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.DirectAimSkill
import org.gitee.orryx.utils.DEFAULT_PICTURE
import org.gitee.orryx.utils.consumeForStartup
import org.gitee.orryx.utils.finishConsumption
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.startSkillAction
import org.gitee.orryx.utils.thenComposeMain
import org.gitee.orryx.utils.toTarget
import taboolib.common5.cdouble
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
        val radius = parameter.runCustomAction(skill.aimRadiusAction).thenApply { it.cdouble }
        val size = parameter.runCustomAction(skill.aimSizeAction).thenApply { it.cdouble }
        return CompletableFuture.allOf(radius, size).thenComposeMain {
            val aimRadius = radius.getNow(Double.NaN)
            val aimSize = size.getNow(Double.NaN)
            require(aimRadius.isFinite() && aimRadius >= 0.0) { "技能瞄准半径必须是非负有限数字" }
            require(aimSize.isFinite() && aimSize >= 0.0) { "技能瞄准尺寸必须是非负有限数字" }
            requestAim(skill, player, parameter, consume, aimRadius, aimSize)
        }
    }

    private fun requestAim(
        skill: DirectAimSkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
        aimRadius: Double,
        aimSize: Double,
    ): CompletableFuture<CastResult> {
        val result = CompletableFuture<CastResult>()
        PluginMessageHandler.requestAiming(player, skill.key, DEFAULT_PICTURE, aimSize, aimRadius) { aimInfo ->
            aimInfo.onSuccess { info ->
                if (info.skillId != skill.key) {
                    result.complete(CastResult.CANCELED)
                    return@onSuccess
                }
                parameter.origin = info.location.toTarget()
                val startup = if (consume) {
                    skill.consumeForStartup(player, parameter).thenComposeMain { consumption ->
                        if (consumption.result == CastResult.SUCCESS) {
                            parameter.finishConsumption(consumption) {
                                parameter.startSkillAction(mapOf("aimRadius" to aimRadius, "aimSize" to aimSize))
                            }
                        } else {
                            CompletableFuture.completedFuture(consumption.result)
                        }
                    }
                } else {
                    parameter.startSkillAction(mapOf("aimRadius" to aimRadius, "aimSize" to aimSize))
                        .thenApply { CastResult.SUCCESS }
                }
                startup.whenComplete { castResult, throwable ->
                    if (throwable == null) result.complete(castResult) else result.completeExceptionally(throwable)
                }
            }.onFailure { result.completeExceptionally(it) }
        }
        return result
    }
}

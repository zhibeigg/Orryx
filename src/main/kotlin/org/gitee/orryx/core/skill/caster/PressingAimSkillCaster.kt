package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.message.PluginMessageHandler
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.skills.PressingAimSkill
import org.gitee.orryx.utils.DEFAULT_PICTURE
import org.gitee.orryx.utils.consumeForStartup
import org.gitee.orryx.utils.finishConsumption
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.startSkillAction
import org.gitee.orryx.utils.thenComposeMain
import org.gitee.orryx.utils.toTarget
import taboolib.common5.cdouble
import taboolib.common5.clong
import java.util.concurrent.CompletableFuture

/** 蓄力指向性技能释放器。 */
object PressingAimSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean = skill is PressingAimSkill

    override fun cast(
        skill: ISkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
    ): CompletableFuture<CastResult> {
        skill as PressingAimSkill
        val radius = parameter.runCustomAction(skill.aimRadiusAction).thenApply { it.cdouble }
        val min = parameter.runCustomAction(skill.aimMinAction).thenApply { it.cdouble }
        val max = parameter.runCustomAction(skill.aimMaxAction).thenApply { it.cdouble }
        val maxTick = parameter.runCustomAction(skill.maxPressTickAction).thenApply { it.clong.coerceAtLeast(0L) }
        return CompletableFuture.allOf(radius, min, max, maxTick).thenComposeMain {
            val aimRadius = radius.getNow(Double.NaN)
            val aimMin = min.getNow(Double.NaN)
            val aimMax = max.getNow(Double.NaN)
            val pressTicks = maxTick.getNow(0L)
            require(aimRadius.isFinite() && aimRadius >= 0.0) { "技能瞄准半径必须是非负有限数字" }
            require(aimMin.isFinite() && aimMax.isFinite() && aimMin >= 0.0 && aimMax >= aimMin) {
                "技能瞄准范围必须满足 0 <= min <= max"
            }
            requestAim(skill, player, parameter, consume, aimRadius, aimMin, aimMax, pressTicks)
        }
    }

    private fun requestAim(
        skill: PressingAimSkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
        aimRadius: Double,
        aimMin: Double,
        aimMax: Double,
        maxTick: Long,
    ): CompletableFuture<CastResult> {
        val result = CompletableFuture<CastResult>()
        val startedAt = System.currentTimeMillis()
        PluginMessageHandler.requestAiming(
            player,
            skill.key,
            DEFAULT_PICTURE,
            aimMin,
            aimMax,
            aimRadius,
            maxTick,
        ) { aimInfo ->
            aimInfo.onSuccess { info ->
                if (info.skillId != skill.key) {
                    result.complete(CastResult.CANCELED)
                    return@onSuccess
                }
                parameter.origin = info.location.toTarget()
                val pressTick = ((info.timestamp - startedAt).coerceAtLeast(0L) / 50L).coerceAtMost(maxTick)
                val variables = mapOf(
                    "aimRadius" to aimRadius,
                    "aimMin" to aimMin,
                    "aimMax" to aimMax,
                    "pressTick" to pressTick,
                )
                val startup = if (consume) {
                    skill.consumeForStartup(player, parameter).thenComposeMain { consumption ->
                        if (consumption.result == CastResult.SUCCESS) {
                            parameter.finishConsumption(consumption) { parameter.startSkillAction(variables) }
                        } else {
                            CompletableFuture.completedFuture(consumption.result)
                        }
                    }
                } else {
                    parameter.startSkillAction(variables).thenApply { CastResult.SUCCESS }
                }
                startup.whenComplete { castResult, throwable ->
                    if (throwable == null) result.complete(castResult) else result.completeExceptionally(throwable)
                }
            }.onFailure { result.completeExceptionally(it) }
        }
        return result
    }
}

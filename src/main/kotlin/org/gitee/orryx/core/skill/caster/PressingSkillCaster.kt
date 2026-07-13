package org.gitee.orryx.core.skill.caster

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStartEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStopEvent
import org.gitee.orryx.api.events.player.press.OrryxPlayerPressTickEvent
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.core.skill.CastResult
import org.gitee.orryx.core.skill.ISkill
import org.gitee.orryx.core.skill.PressSkillManager
import org.gitee.orryx.core.skill.skills.PressingSkill
import org.gitee.orryx.core.station.pipe.PipeBuilder
import org.gitee.orryx.utils.consumeForStartup
import org.gitee.orryx.utils.finishConsumption
import org.gitee.orryx.utils.runCustomAction
import org.gitee.orryx.utils.startSkillAction
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import taboolib.common5.clong
import taboolib.platform.util.sendLang
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** 蓄力技能释放器。 */
object PressingSkillCaster : ISkillCaster {

    override fun supports(skill: ISkill): Boolean = skill is PressingSkill

    override fun cast(
        skill: ISkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
    ): CompletableFuture<CastResult> {
        skill as PressingSkill
        if (PressSkillManager.pressTaskMap.containsKey(player.uniqueId)) {
            return CompletableFuture.completedFuture(CastResult.PRESSING)
        }
        return parameter.runCustomAction(skill.maxPressTickAction).thenApply { value ->
            value.clong.coerceAtLeast(0L)
        }.thenComposeMain { maxPressTick ->
            startPress(skill, player, parameter, consume, maxPressTick)
        }
    }

    private fun startPress(
        skill: PressingSkill,
        player: Player,
        parameter: SkillParameter,
        consume: Boolean,
        maxPressTick: Long,
    ): CompletableFuture<CastResult> {
        val result = CompletableFuture<CastResult>()
        val startedAt = System.currentTimeMillis()
        lateinit var taskId: UUID
        val task = PipeBuilder()
            .uuid(UUID.randomUUID().also { taskId = it })
            .timeout(maxPressTick)
            .brokeTriggers(*skill.pressBrockTriggers)
            .periodTaskAsync(skill.period.coerceAtLeast(1L)) {
                val pressTick = (System.currentTimeMillis() - startedAt) / 50L
                parameter.runCustomAction(skill.pressPeriodAction, mapOf("pressTick" to pressTick))
                    .thenApplyMain {
                        OrryxPlayerPressTickEvent(player, skill, skill.period, pressTick, maxPressTick).call()
                        it
                    }
            }.onComplete {
                val startup = if (consume) {
                    skill.consumeForStartup(player, parameter).thenComposeMain { consumption ->
                        if (consumption.result == CastResult.SUCCESS) {
                            val pressTick = (System.currentTimeMillis() - startedAt) / 50L
                            parameter.finishConsumption(consumption) {
                                parameter.startSkillAction(mapOf("pressTick" to pressTick))
                            }
                        } else {
                            CompletableFuture.completedFuture(consumption.result)
                        }
                    }
                } else {
                    val pressTick = (System.currentTimeMillis() - startedAt) / 50L
                    parameter.startSkillAction(mapOf("pressTick" to pressTick)).thenApply { CastResult.SUCCESS }
                }
                startup.whenComplete { castResult, throwable ->
                    try {
                        org.gitee.orryx.utils.runOnMainThread {
                            val pressTick = (System.currentTimeMillis() - startedAt) / 50L
                            var failure = throwable
                            PressSkillManager.remove(player.uniqueId, taskId)
                            try {
                                OrryxPlayerPressStopEvent(player, skill, pressTick, maxPressTick).call()
                            } catch (ex: Throwable) {
                                if (failure == null) failure = ex else if (failure !== ex) failure?.addSuppressed(ex)
                            }
                            if (failure == null) result.complete(castResult) else result.completeExceptionally(failure)
                        }
                    } catch (scheduleFailure: Throwable) {
                        PressSkillManager.remove(player.uniqueId, taskId)
                        if (throwable != null && throwable !== scheduleFailure) throwable.addSuppressed(scheduleFailure)
                        result.completeExceptionally(throwable ?: scheduleFailure)
                    }
                }
                result.thenApply<Any?> { it }
            }.onBrock {
                val pressTick = (System.currentTimeMillis() - startedAt) / 50L
                var failure: Throwable? = null
                try {
                    player.sendLang("pressing-broke", skill.name)
                    OrryxPlayerPressStopEvent(player, skill, pressTick, maxPressTick).call()
                } catch (ex: Throwable) {
                    failure = ex
                } finally {
                    PressSkillManager.remove(player.uniqueId, taskId)
                }
                if (failure == null) result.complete(CastResult.CANCELED) else result.completeExceptionally(failure)
                result.thenApply { null }
            }.buildPaused()

        if (!PressSkillManager.register(player.uniqueId, skill.key, task)) {
            task.close { CompletableFuture.completedFuture(null) }
            result.complete(CastResult.PRESSING)
            return result
        }
        try {
            task.start()
            OrryxPlayerPressStartEvent(player, skill, maxPressTick).call()
        } catch (throwable: Throwable) {
            PressSkillManager.remove(player.uniqueId, taskId)
            task.fail(throwable).whenComplete { _, closeFailure ->
                if (closeFailure != null && closeFailure !== throwable) throwable.addSuppressed(closeFailure)
                result.completeExceptionally(throwable)
            }
        }
        return result
    }
}

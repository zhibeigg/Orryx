package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.ProfileAPI
import org.gitee.orryx.core.common.timer.CooldownApplication
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.mana.ManaDebitResult
import org.gitee.orryx.module.mana.ManaMangerDefault
import org.gitee.orryx.module.mana.ManaResult
import org.gitee.orryx.module.state.StateManager
import org.gitee.orryx.module.state.states.SkillState
import org.gitee.orryx.utils.SkillSilenceApplication
import org.gitee.orryx.utils.applySilence
import org.gitee.orryx.utils.commitSilenceState
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.mainThreadFuture
import org.gitee.orryx.utils.thenComposeMain
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap

internal data class StartupConsumption(
    val result: CastResult,
    internal val token: UUID?,
)

/** 同一玩家的施法事务严格串行，避免并发检查同时通过。 */
internal object SkillCastCoordinator {

    private data class ConsumptionPlan(
        val mana: Double,
        val cooldown: Long,
        val silence: Long,
    )

    private class AppliedConsumption(
        val player: Player,
        val parameter: SkillParameter,
        val skillKey: String,
        val mana: Double,
        val cooldown: CooldownApplication,
        val silence: SkillSilenceApplication,
    )

    private val pendingConsumptions = ConcurrentHashMap<UUID, AppliedConsumption>()
    private val tails = ConcurrentHashMap<UUID, CompletableFuture<Unit>>()

    fun <T> enqueue(player: UUID, operation: () -> CompletableFuture<T>): CompletableFuture<T> {
        lateinit var result: CompletableFuture<T>
        lateinit var tail: CompletableFuture<Unit>
        tails.compute(player) { _, previous ->
            val ready = previous?.handle { _, _ -> Unit } ?: CompletableFuture.completedFuture(Unit)
            result = ready.thenCompose {
                try {
                    operation()
                } catch (throwable: Throwable) {
                    failed(throwable)
                }
            }
            tail = result.handle { _, _ -> Unit }
            tail
        }
        tail.whenComplete { _, _ -> tails.remove(player, tail) }
        return result
    }

    fun consume(player: Player, parameter: SkillParameter): CompletableFuture<CastResult> {
        return consumeInternal(player, parameter, false).thenApply { it.result }
    }

    fun consumeForStartup(player: Player, parameter: SkillParameter): CompletableFuture<StartupConsumption> {
        return consumeInternal(player, parameter, true)
    }

    private fun consumeInternal(
        player: Player,
        parameter: SkillParameter,
        deferCommitUntilStartup: Boolean,
    ): CompletableFuture<StartupConsumption> {
        val skillKey = parameter.skill
            ?: return CompletableFuture.completedFuture(StartupConsumption(CastResult.PARAMETER, null))
        return player.getSkill(skillKey, false).thenComposeMain { playerSkill ->
            if (playerSkill == null) {
                return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(CastResult.PARAMETER, null))
            }
            if (!SkillTimer.hasNext(player, skillKey)) {
                return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(CastResult.COOLDOWN, null))
            }
            val castSkill = playerSkill.skill as? ICastSkill
                ?: return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(CastResult.PARAMETER, null))
            if (!player.isOnline || player.isDead || playerSkill.locked || playerSkill.level != parameter.level) {
                return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(CastResult.CANCELED, null))
            }
            if (!castSkill.ignoreSilence && Orryx.api().profileAPI.isSilence(player)) {
                return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(CastResult.SILENCE, null))
            }
            buildPlan(parameter).thenComposeMain { plan ->
                require(plan.mana.isFinite() && plan.mana >= 0.0) { "技能法力消耗必须是非负有限数字" }
                if (plan.silence >= 0L && StateManager.getGlobalState(skillKey) !is SkillState) {
                    throw IllegalStateException("技能 $skillKey 缺少对应的 SkillState")
                }

                val consumeFuture = if (plan.mana == 0.0) {
                    CompletableFuture.completedFuture(ManaDebitResult(ManaResult.SUCCESS, 0.0))
                } else {
                    IManaManager.INSTANCE.takeManaDetailed(player, plan.mana)
                }
                consumeFuture.thenComposeMain { debit ->
                    if (debit.result != ManaResult.SUCCESS) {
                        val result = if (debit.result == ManaResult.CANCELLED) {
                            CastResult.CANCELED
                        } else {
                            CastResult.MANA_NOT_ENOUGH
                        }
                        return@thenComposeMain CompletableFuture.completedFuture(StartupConsumption(result, null))
                    }
                    var applied: AppliedConsumption? = null
                    var cooldownApplication: CooldownApplication? = null
                    var silenceApplication: SkillSilenceApplication? = null
                    try {
                        cooldownApplication = SkillTimer.apply(playerSkill, plan.cooldown)
                        silenceApplication = castSkill.applySilence(parameter, player, plan.silence)
                        applied = AppliedConsumption(
                            player,
                            parameter,
                            skillKey,
                            debit.amount,
                            cooldownApplication,
                            silenceApplication,
                        )
                        val token = if (deferCommitUntilStartup) {
                            UUID.randomUUID().also { receipt -> pendingConsumptions[receipt] = applied }
                        } else {
                            commitPreparedState(applied)
                            null
                        }
                        CompletableFuture.completedFuture(StartupConsumption(CastResult.SUCCESS, token))
                    } catch (throwable: Throwable) {
                        val transaction = applied
                        if (transaction != null) {
                            restoreAppliedState(transaction, throwable)
                        } else {
                            restorePartialState(player, skillKey, cooldownApplication, silenceApplication, throwable)
                        }
                        compensateMana(player, debit.amount, throwable)
                    }
                }
            }
        }
    }

    fun commitConsumption(consumption: StartupConsumption): CompletableFuture<CastResult> {
        val token = consumption.token ?: return CompletableFuture.completedFuture(CastResult.SUCCESS)
        val applied = pendingConsumptions[token]
            ?: return failed(IllegalStateException("技能消费事务不存在或已经结束"))
        return mainThreadFuture {
            commitPreparedState(applied)
            pendingConsumptions.remove(token, applied)
            CastResult.SUCCESS
        }
    }

    fun rollbackConsumption(
        consumption: StartupConsumption,
        originalFailure: Throwable,
    ): CompletableFuture<CastResult> {
        val token = consumption.token ?: return failed(originalFailure)
        val applied = pendingConsumptions.remove(token) ?: return failed(originalFailure)
        return mainThreadFuture { restoreAppliedState(applied, originalFailure) }.thenCompose {
            compensateMana(applied.player, applied.mana, originalFailure)
        }
    }

    private fun commitPreparedState(applied: AppliedConsumption) {
        if (applied.silence.plannedState != null) {
            commitSilenceState(applied.player, applied.parameter, applied.silence)
        }
        SkillTimer.commit(applied.cooldown)
    }

    private fun restorePartialState(
        player: Player,
        skillKey: String,
        cooldown: CooldownApplication?,
        silence: SkillSilenceApplication?,
        originalFailure: Throwable,
    ) {
        if (cooldown != null) {
            try {
                SkillTimer.restore(player, skillKey, cooldown)
            } catch (restoreFailure: Throwable) {
                if (restoreFailure !== originalFailure) originalFailure.addSuppressed(restoreFailure)
            }
        }
        val timedStatus = silence?.timedStatus ?: return
        try {
            ProfileAPI.restoreSilenceTransaction(player, timedStatus)
        } catch (restoreFailure: Throwable) {
            if (restoreFailure !== originalFailure) originalFailure.addSuppressed(restoreFailure)
        }
    }

    private fun restoreAppliedState(applied: AppliedConsumption, originalFailure: Throwable) {
        try {
            SkillTimer.restore(applied.player, applied.skillKey, applied.cooldown)
        } catch (restoreFailure: Throwable) {
            if (restoreFailure !== originalFailure) originalFailure.addSuppressed(restoreFailure)
        }
        try {
            val timedStatus = applied.silence.timedStatus
            if (timedStatus != null) {
                ProfileAPI.restoreSilenceTransaction(applied.player, timedStatus)
            }
        } catch (restoreFailure: Throwable) {
            if (restoreFailure !== originalFailure) originalFailure.addSuppressed(restoreFailure)
        }
    }

    private fun buildPlan(parameter: SkillParameter): CompletableFuture<ConsumptionPlan> {
        return parameter.manaValueFuture(true).thenCompose { mana ->
            parameter.cooldownValueFuture(true).thenCompose { cooldown ->
                parameter.silenceValueFuture(true).thenApply { silence ->
                    ConsumptionPlan(mana, cooldown, silence)
                }
            }
        }
    }

    private fun <T> compensateMana(
        player: Player,
        mana: Double,
        originalFailure: Throwable,
    ): CompletableFuture<T> {
        if (mana <= 0.0) return failed(originalFailure)
        val refund = (IManaManager.INSTANCE as? ManaMangerDefault)?.refundManaExact(player, mana)
            ?: IManaManager.INSTANCE.giveMana(player, mana).thenApply { result ->
                check(result == ManaResult.SUCCESS) { "法力补偿失败: $result" }
                Unit
            }
        return refund.handle { _, refundFailure ->
            if (refundFailure != null) originalFailure.addSuppressed(unwrap(refundFailure))
            throw CompletionException(originalFailure)
        }
    }

    private fun unwrap(throwable: Throwable): Throwable {
        var current = throwable
        while (current is CompletionException && current.cause != null) {
            current = current.cause ?: break
        }
        return current
    }

    private fun <T> failed(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }
}

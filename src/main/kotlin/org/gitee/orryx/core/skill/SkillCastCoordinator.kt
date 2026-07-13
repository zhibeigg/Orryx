package org.gitee.orryx.core.skill

import org.bukkit.entity.Player
import org.gitee.orryx.core.common.timer.SkillTimer
import org.gitee.orryx.core.kether.parameter.SkillParameter
import org.gitee.orryx.module.mana.IManaManager
import org.gitee.orryx.module.mana.ManaResult
import org.gitee.orryx.utils.getSkill
import org.gitee.orryx.utils.silence
import org.gitee.orryx.utils.thenComposeMain
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/** 同一玩家的施法事务严格串行，避免并发检查同时通过。 */
internal object SkillCastCoordinator {

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
        val skillKey = parameter.skill ?: return CompletableFuture.completedFuture(CastResult.PARAMETER)
        return player.getSkill(skillKey, false).thenComposeMain { playerSkill ->
            if (playerSkill == null) return@thenComposeMain CompletableFuture.completedFuture(CastResult.PARAMETER)
            if (!SkillTimer.hasNext(player, skillKey)) {
                return@thenComposeMain CompletableFuture.completedFuture(CastResult.COOLDOWN)
            }
            val cost = parameter.manaValue(true).takeIf { it.isFinite() }?.coerceAtLeast(0.0)
                ?: return@thenComposeMain CompletableFuture.completedFuture(CastResult.MANA_NOT_ENOUGH)
            val consumeFuture = if (cost <= 0.0) {
                CompletableFuture.completedFuture(ManaResult.SUCCESS)
            } else {
                IManaManager.INSTANCE.takeMana(player, cost)
            }
            consumeFuture.thenComposeMain { result ->
                if (result != ManaResult.SUCCESS) {
                    return@thenComposeMain CompletableFuture.completedFuture(
                        if (result == ManaResult.CANCELLED) CastResult.CANCELED else CastResult.MANA_NOT_ENOUGH
                    )
                }
                SkillTimer.reset(playerSkill, parameter)
                (playerSkill.skill as? ICastSkill)?.silence(parameter, player)
                CompletableFuture.completedFuture(CastResult.SUCCESS)
            }
        }
    }

    private fun <T> failed(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }
}

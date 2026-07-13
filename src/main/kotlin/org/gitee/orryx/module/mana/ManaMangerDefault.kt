package org.gitee.orryx.module.mana

import org.bukkit.entity.Player
import org.gitee.nodens.common.DigitalParser.Type.COUNT
import org.gitee.nodens.common.DigitalParser.Type.PERCENT
import org.gitee.nodens.core.attribute.Mana
import org.gitee.nodens.core.entity.EntityAttributeMemory.Companion.attributeMemory
import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.profile.PlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.persistence.PersistenceWriteException
import org.gitee.orryx.utils.MANA_FLAG
import org.gitee.orryx.utils.NodensPlugin
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.orryxProfileTo
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class ManaMangerDefault : IManaManager {

    override fun getMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfileTo { profile -> profile.getFlag(MANA_FLAG)?.value.cdouble }
    }

    override fun getMaxMana(player: Player): CompletableFuture<Double> {
        return player.job().thenApplyMain { job -> job?.getMaxMana() ?: 0.0 }
    }

    override fun getMaxMana(player: Player, job: IJob, level: Int): Double {
        val mana = player.eval(job.maxManaActions, mapOf("level" to level)).orNull().cdouble
        var extend = 0.0
        if (NodensPlugin.isEnabled) {
            val valueMap = player.attributeMemory()?.mergedAttribute(Mana.Max)
            valueMap?.forEach { (type, value) ->
                extend += when (type) {
                    PERCENT -> ((valueMap[COUNT]?.get(0) ?: 0.0) + mana) * value[0]
                    COUNT -> value[0]
                }
            }
        }
        return (mana + extend).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    }

    override fun getMaxMana(player: Player, job: String, level: Int): Double {
        val jobLoader = JobLoaderManager.getJobLoader(job) ?: return 0.0
        return getMaxMana(player, jobLoader, level)
    }

    override fun giveMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (!mana.isFinite()) return failed(IllegalArgumentException("法力值必须是有限数字"))
        if (mana < 0.0) return takeMana(player, -mana)
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.NO_JOB)
                val event = OrryxPlayerManaEvents.Up.Pre(player, profile, mana)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.CANCELLED)
                val max = job.getMaxMana().finiteNonNegative()
                val current = current(profile)
                val next = (current + event.mana.finiteNonNegative()).coerceIn(0.0, max)
                val previous = profile.getFlag(MANA_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerManaEvents.Up.Post(player, profile, event.mana).call()
                    ManaResult.SUCCESS
                }
            }
        }
    }

    override fun takeMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (!mana.isFinite()) return failed(IllegalArgumentException("法力值必须是有限数字"))
        if (mana < 0.0) return giveMana(player, -mana)
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.NO_JOB)
                val event = OrryxPlayerManaEvents.Down.Pre(player, profile, mana)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.CANCELLED)
                val cost = event.mana.finiteNonNegative()
                val current = current(profile)
                if (current < cost) {
                    return@thenComposeMain CompletableFuture.completedFuture(ManaResult.NOT_ENOUGH)
                }
                val next = (current - cost).coerceIn(0.0, job.getMaxMana().finiteNonNegative())
                val previous = profile.getFlag(MANA_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerManaEvents.Down.Post(player, profile, cost).call()
                    ManaResult.SUCCESS
                }
            }
        }
    }

    override fun setMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (!mana.isFinite()) return failed(IllegalArgumentException("法力值必须是有限数字"))
        return getMana(player).thenCompose { current ->
            when {
                mana > current -> giveMana(player, mana - current)
                mana < current -> takeMana(player, current - mana)
                else -> CompletableFuture.completedFuture(ManaResult.SAME)
            }
        }
    }

    override fun haveMana(player: Player, mana: Double): CompletableFuture<Boolean> {
        if (!mana.isFinite()) return CompletableFuture.completedFuture(false)
        if (mana <= 0.0) return CompletableFuture.completedFuture(true)
        return player.orryxProfileTo { profile -> current(profile) + EPSILON >= mana }
    }

    override fun regainMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val max = job.getMaxMana().finiteNonNegative()
                val current = current(profile).coerceAtMost(max)
                if (current + EPSILON >= max) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val configured = job.getRegainMana().finiteNonNegative()
                if (configured <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val event = OrryxPlayerManaEvents.Regain.Pre(player, profile, configured)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val added = event.regainMana.finiteNonNegative().coerceAtMost(max - current)
                if (added <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val previous = profile.getFlag(MANA_FLAG)
                val attempted = (current + added).flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerManaEvents.Regain.Post(player, profile, added).call()
                    added
                }
            }
        }
    }

    override fun healMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val max = job.getMaxMana().finiteNonNegative()
                val current = current(profile).coerceAtMost(max)
                val requested = (max - current).coerceAtLeast(0.0)
                if (requested <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val event = OrryxPlayerManaEvents.Heal.Pre(player, profile, requested)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val added = event.healMana.finiteNonNegative().coerceAtMost(max - current)
                val previous = profile.getFlag(MANA_FLAG)
                val attempted = (current + added).flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerManaEvents.Heal.Post(player, profile, added).call()
                    added
                }
            }
        }
    }

    private fun replaceResourceFlag(profile: IPlayerProfile, player: Player, flag: IFlag?) {
        val concrete = profile as? PlayerProfile
            ?: error("Mana 仅支持 Orryx 内置 PlayerProfile 实现")
        concrete.replaceSystemFlag(player, MANA_FLAG, flag)
    }

    private fun <T> commitResource(
        profile: IPlayerProfile,
        player: Player,
        previous: IFlag?,
        attempted: IFlag,
        committed: () -> T,
    ): CompletableFuture<T> {
        return PersistenceManager.saveProfile(profile.createPO(), invalidate = false)
            .handle { _, throwable -> throwable }
            .thenComposeMain { throwable ->
                if (throwable == null || throwable.databaseCommitted()) {
                    if (throwable != null) throwable.printStackTrace()
                    MemoryCache.savePlayerProfile(profile)
                    CompletableFuture.completedFuture(committed())
                } else {
                    if (profile.getFlag(MANA_FLAG) === attempted) {
                        replaceResourceFlag(profile, player, previous)
                        MemoryCache.savePlayerProfile(profile)
                    }
                    failed(throwable)
                }
            }
    }

    private fun Throwable.databaseCommitted(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is PersistenceWriteException && current.databaseCommitted) return true
            current = current.cause
        }
        return false
    }

    private fun current(profile: IPlayerProfile): Double {
        return profile.getFlag(MANA_FLAG)?.value.cdouble.finiteNonNegative()
    }

    private fun Double.finiteNonNegative(): Double {
        return if (isFinite()) coerceAtLeast(0.0) else 0.0
    }

    private fun <T> failed(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    companion object {
        private const val EPSILON = 1e-9
    }
}

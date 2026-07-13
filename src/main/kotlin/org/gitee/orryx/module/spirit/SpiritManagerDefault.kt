package org.gitee.orryx.module.spirit

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents
import org.gitee.orryx.core.job.IJob
import org.gitee.orryx.core.job.JobLoaderManager
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.profile.PlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.persistence.PersistenceWriteException
import org.gitee.orryx.utils.SPIRIT_FLAG
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import taboolib.common.platform.ProxyCommandSender
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture

class SpiritManagerDefault : ISpiritManager {

    override fun getMaxSpirit(player: Player): CompletableFuture<Double> {
        return player.job().thenApplyMain { job -> job?.getMaxSpirit() ?: 0.0 }
    }

    override fun getMaxSpirit(sender: ProxyCommandSender, job: IJob, level: Int): Double {
        return sender.eval(job.maxSpiritActions, mapOf("level" to level)).orNull().cdouble
            .takeIf { it.isFinite() }
            ?.coerceAtLeast(0.0)
            ?: 0.0
    }

    override fun getMaxSpirit(sender: ProxyCommandSender, job: String, level: Int): Double {
        val jobLoader = JobLoaderManager.getJobLoader(job) ?: return 0.0
        return getMaxSpirit(sender, jobLoader, level)
    }

    override fun getSpirit(player: Player): Double {
        val profile = MemoryCache.getPlayerProfile(player.uniqueId).getNow(null) ?: return 0.0
        return current(profile)
    }

    override fun giveSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (!spirit.isFinite()) return failed(IllegalArgumentException("精力值必须是有限数字"))
        if (spirit < 0.0) return takeSpirit(player, -spirit)
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.NO_JOB)
                val event = OrryxPlayerSpiritEvents.Up.Pre(player, profile, spirit)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.CANCELLED)
                val max = job.getMaxSpirit().finiteNonNegative()
                val next = (current(profile) + event.spirit.finiteNonNegative()).coerceIn(0.0, max)
                val previous = profile.getFlag(SPIRIT_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerSpiritEvents.Up.Post(player, profile, event.spirit).call()
                    SpiritResult.SUCCESS
                }
            }
        }
    }

    override fun takeSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (!spirit.isFinite()) return failed(IllegalArgumentException("精力值必须是有限数字"))
        if (spirit < 0.0) return giveSpirit(player, -spirit)
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.NO_JOB)
                val event = OrryxPlayerSpiritEvents.Down.Pre(player, profile, spirit)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.CANCELLED)
                val cost = event.spirit.finiteNonNegative()
                val current = current(profile)
                if (current < cost) {
                    return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.NOT_ENOUGH)
                }
                val next = (current - cost).coerceIn(0.0, job.getMaxSpirit().finiteNonNegative())
                val previous = profile.getFlag(SPIRIT_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerSpiritEvents.Down.Post(player, profile, cost).call()
                    SpiritResult.SUCCESS
                }
            }
        }
    }

    override fun haveSpirit(player: Player, spirit: Double): Boolean {
        if (!spirit.isFinite()) return false
        return spirit <= 0.0 || getSpirit(player) + EPSILON >= spirit
    }

    override fun healSpirit(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val max = job.getMaxSpirit().finiteNonNegative()
                val current = current(profile).coerceAtMost(max)
                val requested = (max - current).coerceAtLeast(0.0)
                if (requested <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val event = OrryxPlayerSpiritEvents.Heal.Pre(player, profile, requested)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val added = event.healSpirit.finiteNonNegative().coerceAtMost(max - current)
                val previous = profile.getFlag(SPIRIT_FLAG)
                val attempted = (current + added).flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerSpiritEvents.Heal.Post(player, profile, added).call()
                    added
                }
            }
        }
    }

    override fun regainSpirit(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenComposeMain { job ->
                if (job == null) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val max = job.getMaxSpirit().finiteNonNegative()
                val current = current(profile).coerceAtMost(max)
                if (current + EPSILON >= max) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val configured = job.getRegainSpirit().finiteNonNegative()
                if (configured <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val event = OrryxPlayerSpiritEvents.Regain.Pre(player, profile, configured)
                if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val added = event.regainSpirit.finiteNonNegative().coerceAtMost(max - current)
                if (added <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                val previous = profile.getFlag(SPIRIT_FLAG)
                val attempted = (current + added).flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) {
                    OrryxPlayerSpiritEvents.Regain.Post(player, profile, added).call()
                    added
                }
            }
        }
    }

    override fun setSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (!spirit.isFinite()) return failed(IllegalArgumentException("精力值必须是有限数字"))
        return player.orryxProfile().thenComposeMain { profile ->
            val current = current(profile)
            when {
                spirit > current -> giveSpirit(player, spirit - current)
                spirit < current -> takeSpirit(player, current - spirit)
                else -> CompletableFuture.completedFuture(SpiritResult.SAME)
            }
        }
    }

    private fun replaceResourceFlag(profile: IPlayerProfile, player: Player, flag: IFlag?) {
        val concrete = profile as? PlayerProfile
            ?: error("Spirit 仅支持 Orryx 内置 PlayerProfile 实现")
        concrete.replaceSystemFlag(player, SPIRIT_FLAG, flag)
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
                    if (profile.getFlag(SPIRIT_FLAG) === attempted) {
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
        return profile.getFlag(SPIRIT_FLAG)?.value.cdouble.finiteNonNegative()
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

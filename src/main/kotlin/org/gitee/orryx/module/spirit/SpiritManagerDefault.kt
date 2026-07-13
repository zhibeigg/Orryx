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
import org.gitee.orryx.module.PlayerResourceCoordinator
import org.gitee.orryx.utils.SPIRIT_FLAG
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class SpiritManagerDefault : ISpiritManager {

    override fun getMaxSpirit(player: Player): CompletableFuture<Double> {
        return player.job().thenCompose { job ->
            job?.getMaxSpiritAsync() ?: CompletableFuture.completedFuture(0.0)
        }.thenApplyMain { it.finiteNonNegative() }
    }

    override fun getMaxSpirit(sender: ProxyCommandSender, job: IJob, level: Int): Double {
        check(isPrimaryThread) { "同步 getMaxSpirit 必须在 Bukkit 主线程调用" }
        val future = sender.eval(job.maxSpiritActions, mapOf("level" to level))
        check(future.isDone) { "职业 ${job.key} 的 MaxSpirit 不允许包含异步动作" }
        val value = try {
            future.getNow(null).cdouble
        } catch (throwable: CompletionException) {
            throw throwable.cause ?: throwable
        }
        return value.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
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
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            if (spirit < 0.0) takeSpiritTransactionDetailed(player, -spirit).thenApply { it.result } else giveSpiritTransaction(player, spirit)
        }
    }

    private fun giveSpiritTransaction(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(SpiritResult.NO_JOB)
                job.getMaxSpiritAsync().thenComposeMain { configuredMax ->
                    val event = OrryxPlayerSpiritEvents.Up.Pre(player, profile, spirit)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.CANCELLED)
                    val amount = event.spirit.requireNonNegative("事件修改后的精力增加值")
                    val max = configuredMax.finiteNonNegative()
                    val current = current(profile)
                    val next = if (current >= max) current else (current + amount).coerceAtMost(max)
                    val actual = next - current
                    if (actual <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(SpiritResult.SAME)
                    val previous = profile.getFlag(SPIRIT_FLAG)
                    val attempted = next.flag(true)
                    replaceResourceFlag(profile, player, attempted)
                    commitResource(profile, player, previous, attempted) {
                        OrryxPlayerSpiritEvents.Up.Post(player, profile, actual).call()
                        SpiritResult.SUCCESS
                    }
                }
            }
        }
    }

    override fun takeSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        return takeSpiritDetailed(player, spirit).thenApply { it.result }
    }

    override fun takeSpiritDetailed(player: Player, spirit: Double): CompletableFuture<SpiritDebitResult> {
        if (!spirit.isFinite()) return failed(IllegalArgumentException("精力值必须是有限数字"))
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            if (spirit < 0.0) {
                giveSpiritTransaction(player, -spirit).thenApply { SpiritDebitResult(it, 0.0) }
            } else {
                takeSpiritTransactionDetailed(player, spirit)
            }
        }
    }

    private fun takeSpiritTransactionDetailed(player: Player, spirit: Double): CompletableFuture<SpiritDebitResult> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(SpiritDebitResult(SpiritResult.NO_JOB, 0.0))
                job.getMaxSpiritAsync().thenComposeMain { configuredMax ->
                    val event = OrryxPlayerSpiritEvents.Down.Pre(player, profile, spirit)
                    if (!event.call()) {
                        return@thenComposeMain CompletableFuture.completedFuture(SpiritDebitResult(SpiritResult.CANCELLED, 0.0))
                    }
                    val cost = event.spirit.requireNonNegative("事件修改后的精力消耗值")
                    val current = current(profile)
                    if (current < cost) {
                        return@thenComposeMain CompletableFuture.completedFuture(SpiritDebitResult(SpiritResult.NOT_ENOUGH, 0.0))
                    }
                    val next = (current - cost).coerceAtLeast(0.0)
                    val actual = current - next
                    val previous = profile.getFlag(SPIRIT_FLAG)
                    val attempted = next.flag(true)
                    replaceResourceFlag(profile, player, attempted)
                    commitResource(profile, player, previous, attempted) {
                        OrryxPlayerSpiritEvents.Down.Post(player, profile, actual).call()
                        SpiritDebitResult(SpiritResult.SUCCESS, actual)
                    }
                }
            }
        }
    }

    override fun haveSpirit(player: Player, spirit: Double): Boolean {
        if (!spirit.isFinite()) return false
        return spirit <= 0.0 || getSpirit(player) + EPSILON >= spirit
    }

    override fun healSpirit(player: Player): CompletableFuture<Double> {
        return PlayerResourceCoordinator.enqueue(player.uniqueId) { healSpiritTransaction(player) }
    }

    private fun healSpiritTransaction(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(0.0)
                job.getMaxSpiritAsync().thenComposeMain { configuredMax ->
                    val max = configuredMax.finiteNonNegative()
                    val current = current(profile)
                    val requested = (max - current).coerceAtLeast(0.0)
                    if (requested <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val event = OrryxPlayerSpiritEvents.Heal.Pre(player, profile, requested)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val added = event.healSpirit.requireNonNegative("事件修改后的精力治疗值").coerceAtMost(max - current)
                    if (added <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
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
    }

    override fun regainSpirit(player: Player): CompletableFuture<Double> {
        return PlayerResourceCoordinator.enqueue(player.uniqueId) { regainSpiritTransaction(player) }
    }

    private fun regainSpiritTransaction(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(0.0)
                val maxFuture = job.getMaxSpiritAsync()
                val regainFuture = job.getRegainSpiritAsync()
                CompletableFuture.allOf(maxFuture, regainFuture).thenComposeMain {
                    val max = maxFuture.getNow(0.0).finiteNonNegative()
                    val current = current(profile)
                    if (current + EPSILON >= max) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val configured = regainFuture.getNow(0.0).finiteNonNegative()
                    if (configured <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val event = OrryxPlayerSpiritEvents.Regain.Pre(player, profile, configured)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val added = event.regainSpirit.requireNonNegative("事件修改后的精力恢复值").coerceAtMost(max - current)
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
    }

    override fun setSpirit(player: Player, spirit: Double): CompletableFuture<SpiritResult> {
        if (!spirit.isFinite() || spirit < 0.0) {
            return failed(IllegalArgumentException("精力值必须是非负有限数字"))
        }
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            player.orryxProfile().thenCompose { profile ->
                val current = current(profile)
                when {
                    spirit > current -> giveSpiritTransaction(player, spirit - current)
                    spirit < current -> takeSpiritTransactionDetailed(player, current - spirit).thenApply { it.result }
                    else -> CompletableFuture.completedFuture(SpiritResult.SAME)
                }
            }
        }
    }

    internal fun refundSpiritExact(player: Player, spirit: Double): CompletableFuture<Unit> {
        if (!spirit.isFinite() || spirit < 0.0) return failed(IllegalArgumentException("精力补偿值非法"))
        if (spirit == 0.0) return CompletableFuture.completedFuture(Unit)
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            player.orryxProfile().thenComposeMain { profile ->
                val next = current(profile) + spirit
                require(next.isFinite()) { "精力补偿结果溢出" }
                val previous = profile.getFlag(SPIRIT_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) { Unit }
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
        val persistence = try {
            PersistenceManager.saveProfile(profile.createPO(), invalidate = false)
        } catch (throwable: Throwable) {
            failed<Unit>(throwable)
        }
        return persistence.handle { _, throwable -> throwable }
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

    private fun Double.requireNonNegative(label: String): Double {
        require(isFinite() && this >= 0.0) { "$label 必须是非负有限数字" }
        return this
    }

    private fun <T> failed(throwable: Throwable): CompletableFuture<T> {
        return CompletableFuture<T>().also { it.completeExceptionally(throwable) }
    }

    companion object {
        private const val EPSILON = 1e-9
    }
}

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
import org.gitee.orryx.module.PlayerResourceCoordinator
import org.gitee.orryx.utils.MANA_FLAG
import org.gitee.orryx.utils.NodensPlugin
import org.gitee.orryx.utils.eval
import org.gitee.orryx.utils.flag
import org.gitee.orryx.utils.job
import org.gitee.orryx.utils.orryxProfile
import org.gitee.orryx.utils.orryxProfileTo
import org.gitee.orryx.utils.thenApplyMain
import org.gitee.orryx.utils.thenComposeMain
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common5.cdouble
import taboolib.module.kether.orNull
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class ManaMangerDefault : IManaManager {

    override fun getMana(player: Player): CompletableFuture<Double> {
        return player.orryxProfileTo { profile -> profile.getFlag(MANA_FLAG)?.value.cdouble }
    }

    override fun getMaxMana(player: Player): CompletableFuture<Double> {
        return player.job().thenCompose { job ->
            job?.getMaxManaAsync() ?: CompletableFuture.completedFuture(0.0)
        }.thenApplyMain { it.finiteNonNegative() }
    }

    override fun getMaxMana(player: Player, job: IJob, level: Int): Double {
        check(isPrimaryThread) { "同步 getMaxMana 必须在 Bukkit 主线程调用" }
        val future = player.eval(job.maxManaActions, mapOf("level" to level))
        check(future.isDone) { "职业 ${job.key} 的 MaxMana 不允许包含异步动作" }
        val mana = try {
            future.getNow(null).cdouble
        } catch (throwable: CompletionException) {
            throw throwable.cause ?: throwable
        }
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
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            if (mana < 0.0) {
                takeManaTransactionDetailed(player, -mana).thenApply { it.result }
            } else {
                giveManaTransaction(player, mana)
            }
        }
    }

    private fun giveManaTransaction(player: Player, mana: Double): CompletableFuture<ManaResult> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(ManaResult.NO_JOB)
                job.getMaxManaAsync().thenComposeMain { configuredMax ->
                    val event = OrryxPlayerManaEvents.Up.Pre(player, profile, mana)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.CANCELLED)
                    val amount = event.mana.requireNonNegative("事件修改后的法力增加值")
                    val max = configuredMax.finiteNonNegative()
                    val current = current(profile)
                    val next = if (current >= max) current else (current + amount).coerceAtMost(max)
                    val actual = next - current
                    if (actual <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(ManaResult.SAME)
                    val previous = profile.getFlag(MANA_FLAG)
                    val attempted = next.flag(true)
                    replaceResourceFlag(profile, player, attempted)
                    commitResource(profile, player, previous, attempted) {
                        OrryxPlayerManaEvents.Up.Post(player, profile, actual).call()
                        ManaResult.SUCCESS
                    }
                }
            }
        }
    }

    override fun takeMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        return takeManaDetailed(player, mana).thenApply { it.result }
    }

    override fun takeManaDetailed(player: Player, mana: Double): CompletableFuture<ManaDebitResult> {
        if (!mana.isFinite()) return failed(IllegalArgumentException("法力值必须是有限数字"))
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            if (mana < 0.0) {
                giveManaTransaction(player, -mana).thenApply { ManaDebitResult(it, 0.0) }
            } else {
                takeManaTransactionDetailed(player, mana)
            }
        }
    }

    private fun takeManaTransactionDetailed(player: Player, mana: Double): CompletableFuture<ManaDebitResult> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(ManaDebitResult(ManaResult.NO_JOB, 0.0))
                job.getMaxManaAsync().thenComposeMain { configuredMax ->
                    val event = OrryxPlayerManaEvents.Down.Pre(player, profile, mana)
                    if (!event.call()) {
                        return@thenComposeMain CompletableFuture.completedFuture(ManaDebitResult(ManaResult.CANCELLED, 0.0))
                    }
                    val cost = event.mana.requireNonNegative("事件修改后的法力消耗值")
                    val current = current(profile)
                    if (current < cost) {
                        return@thenComposeMain CompletableFuture.completedFuture(
                            ManaDebitResult(ManaResult.NOT_ENOUGH, 0.0)
                        )
                    }
                    val next = (current - cost).coerceAtLeast(0.0)
                    val actual = current - next
                    val previous = profile.getFlag(MANA_FLAG)
                    val attempted = next.flag(true)
                    replaceResourceFlag(profile, player, attempted)
                    commitResource(profile, player, previous, attempted) {
                        OrryxPlayerManaEvents.Down.Post(player, profile, actual).call()
                        ManaDebitResult(ManaResult.SUCCESS, actual)
                    }
                }
            }
        }
    }

    override fun setMana(player: Player, mana: Double): CompletableFuture<ManaResult> {
        if (!mana.isFinite() || mana < 0.0) {
            return failed(IllegalArgumentException("法力值必须是非负有限数字"))
        }
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            player.orryxProfile().thenCompose { profile ->
                val current = current(profile)
                when {
                    mana > current -> giveManaTransaction(player, mana - current)
                    mana < current -> takeManaTransactionDetailed(player, current - mana).thenApply { it.result }
                    else -> CompletableFuture.completedFuture(ManaResult.SAME)
                }
            }
        }
    }

    override fun haveMana(player: Player, mana: Double): CompletableFuture<Boolean> {
        if (!mana.isFinite()) return CompletableFuture.completedFuture(false)
        if (mana <= 0.0) return CompletableFuture.completedFuture(true)
        return player.orryxProfileTo { profile -> current(profile) + EPSILON >= mana }
    }

    override fun regainMana(player: Player): CompletableFuture<Double> {
        return PlayerResourceCoordinator.enqueue(player.uniqueId) { regainManaTransaction(player) }
    }

    private fun regainManaTransaction(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(0.0)
                val maxFuture = job.getMaxManaAsync()
                val regainFuture = job.getRegainManaAsync()
                CompletableFuture.allOf(maxFuture, regainFuture).thenComposeMain {
                    val max = maxFuture.getNow(0.0).finiteNonNegative()
                    val current = current(profile)
                    if (current + EPSILON >= max) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val configured = regainFuture.getNow(0.0).finiteNonNegative()
                    if (configured <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val event = OrryxPlayerManaEvents.Regain.Pre(player, profile, configured)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val added = event.regainMana.requireNonNegative("事件修改后的法力恢复值").coerceAtMost(max - current)
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
    }

    override fun healMana(player: Player): CompletableFuture<Double> {
        return PlayerResourceCoordinator.enqueue(player.uniqueId) { healManaTransaction(player) }
    }

    private fun healManaTransaction(player: Player): CompletableFuture<Double> {
        return player.orryxProfile().thenComposeMain { profile ->
            player.job().thenCompose { job ->
                if (job == null) return@thenCompose CompletableFuture.completedFuture(0.0)
                job.getMaxManaAsync().thenComposeMain { configuredMax ->
                    val max = configuredMax.finiteNonNegative()
                    val current = current(profile)
                    val requested = (max - current).coerceAtLeast(0.0)
                    if (requested <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val event = OrryxPlayerManaEvents.Heal.Pre(player, profile, requested)
                    if (!event.call()) return@thenComposeMain CompletableFuture.completedFuture(0.0)
                    val added = event.healMana.requireNonNegative("事件修改后的法力治疗值").coerceAtMost(max - current)
                    if (added <= 0.0) return@thenComposeMain CompletableFuture.completedFuture(0.0)
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
    }

    internal fun refundManaExact(player: Player, mana: Double): CompletableFuture<Unit> {
        if (!mana.isFinite() || mana < 0.0) return failed(IllegalArgumentException("法力补偿值非法"))
        if (mana == 0.0) return CompletableFuture.completedFuture(Unit)
        return PlayerResourceCoordinator.enqueue(player.uniqueId) {
            player.orryxProfile().thenComposeMain { profile ->
                val next = current(profile) + mana
                require(next.isFinite()) { "法力补偿结果溢出" }
                val previous = profile.getFlag(MANA_FLAG)
                val attempted = next.flag(true)
                replaceResourceFlag(profile, player, attempted)
                commitResource(profile, player, previous, attempted) { Unit }
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

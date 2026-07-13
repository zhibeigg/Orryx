package org.gitee.orryx.core.profile

import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.events.player.OrryxPlayerFlagChangeEvents
import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.api.events.player.OrryxPlayerProfileSaveEvents
import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.GameManager
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.persistence.PersistenceManager
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.persistence.PersistenceWriteException
import org.gitee.orryx.module.PlayerResourceCoordinator
import org.gitee.orryx.utils.finishSaveCallback
import org.gitee.orryx.utils.mainThreadFuture
import org.gitee.orryx.utils.runOnMainThread
import org.gitee.orryx.utils.thenComposeMain
import org.gitee.orryx.utils.toSerializable
import taboolib.common.platform.function.isPrimaryThread
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentMap

class PlayerProfile(
    override val id: Int,
    override val uuid: UUID,
    private var privateJob: String?,
    private var privatePoint: Int,
    private val privateFlags: ConcurrentMap<String, IFlag>
): IPlayerProfile {

    constructor(id: Int, player: Player, privateJob: String?, privatePoint: Int, privateFlags: ConcurrentMap<String, IFlag>): this(id, player.uniqueId, privateJob, privatePoint, privateFlags)

    override val player: Player
        get() = Bukkit.getPlayer(uuid) ?: error("Player Offline")

    override val flags: Map<String, IFlag>
        get() = privateFlags.entries.mapNotNull { (key, flag) ->
            if (flag.isTimeout()) {
                removeFlagIfSameAsync(key, flag).exceptionally { it.printStackTrace(); null }
                null
            } else {
                key to flag
            }
        }.toMap()

    override val job: String?
        get() = privateJob

    override val point: Int
        get() = privatePoint

    override fun setFlag(flagName: String, flag: IFlag, save: Boolean) {
        setFlagAsync(flagName, flag, save).exceptionally { it.printStackTrace(); null }
    }

    internal fun setFlagAsync(flagName: String, flag: IFlag, save: Boolean = true): CompletableFuture<Boolean> {
        return mutateFlag(flagName, flag, save).thenApply { it != null }
    }

    override fun getFlag(flagName: String): IFlag? {
        val flag = privateFlags[flagName] ?: return null
        if (!flag.isTimeout()) return flag
        removeFlagIfSameAsync(flagName, flag).exceptionally { it.printStackTrace(); null }
        return null
    }

    /** 系统保留 Flag 的内部写入口，不触发可取消的通用 Flag 事件。 */
    internal fun replaceSystemFlag(player: Player, flagName: String, flag: IFlag?) {
        require(player.uniqueId == uuid) { "玩家与 Profile 不匹配" }
        privateFlags.remove(flagName)?.cancel(player, flagName)
        if (flag != null) {
            privateFlags[flagName] = flag
            flag.init(player, flagName)
        }
    }

    override fun removeFlag(flagName: String, save: Boolean): IFlag? {
        val previous = getFlag(flagName)
        removeFlagAsync(flagName, save).exceptionally { it.printStackTrace(); null }
        return previous
    }

    internal fun removeFlagAsync(flagName: String, save: Boolean = true): CompletableFuture<IFlag?> {
        return mutateFlag(flagName, null, save).thenApply { it?.reportedOldFlag }
    }

    internal fun removeFlagIfSameAsync(flagName: String, expected: IFlag): CompletableFuture<IFlag?> {
        return mutateFlag(flagName, null, true, expected).thenApply { it?.reportedOldFlag }
    }

    private fun mutateFlag(
        flagName: String,
        requested: IFlag?,
        save: Boolean,
        expected: IFlag? = null,
    ): CompletableFuture<FlagMutation?> {
        return PlayerResourceCoordinator.enqueue(uuid) {
            mainThreadFuture {
                if (expected != null && privateFlags[flagName] !== expected) return@mainThreadFuture null
                val onlinePlayer = player
                val event = OrryxPlayerFlagChangeEvents.Pre(
                    onlinePlayer,
                    this,
                    flagName,
                    privateFlags[flagName],
                    requested,
                )
                if (!event.call()) return@mainThreadFuture null

                val effectiveKey = event.flagName
                val originalFlag = privateFlags[flagName]
                val targetFlag = if (effectiveKey == flagName) originalFlag else privateFlags[effectiveKey]
                privateFlags.remove(flagName)?.cancel(onlinePlayer, flagName)
                if (effectiveKey != flagName) {
                    privateFlags.remove(effectiveKey)?.cancel(onlinePlayer, effectiveKey)
                }
                event.newFlag?.let {
                    privateFlags[effectiveKey] = it
                    it.init(onlinePlayer, effectiveKey)
                }
                FlagMutation(
                    onlinePlayer,
                    flagName,
                    effectiveKey,
                    originalFlag,
                    targetFlag,
                    event.oldFlag,
                    event.newFlag,
                    save,
                )
            }.thenCompose { mutation ->
                if (mutation == null) return@thenCompose CompletableFuture.completedFuture(null)
                val requiresSave = mutation.save && listOf(
                    mutation.originalFlag,
                    mutation.targetFlag,
                    mutation.newFlag,
                ).any { it?.isPersistence == true }
                val persistence = if (requiresSave) {
                    try {
                        PersistenceManager.saveProfile(createPO(), invalidate = false)
                    } catch (throwable: Throwable) {
                        CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
                    }
                } else {
                    CompletableFuture.completedFuture(Unit)
                }
                persistence.handle { _, throwable -> throwable }.thenComposeMain { throwable ->
                    if (throwable == null || throwable.databaseCommitted()) {
                        throwable?.printStackTrace()
                        OrryxPlayerFlagChangeEvents.Post(
                            mutation.player,
                            this,
                            mutation.targetKey,
                            mutation.reportedOldFlag,
                            mutation.newFlag,
                        ).call()
                        CompletableFuture.completedFuture(mutation)
                    } else {
                        restoreFlagMutation(mutation)
                        CompletableFuture<FlagMutation?>().also { it.completeExceptionally(throwable) }
                    }
                }
            }
        }
    }

    private fun restoreFlagMutation(mutation: FlagMutation) {
        privateFlags.remove(mutation.targetKey)?.cancel(mutation.player, mutation.targetKey)
        restoreFlag(mutation.player, mutation.originalKey, mutation.originalFlag)
        if (mutation.targetKey != mutation.originalKey) {
            restoreFlag(mutation.player, mutation.targetKey, mutation.targetFlag)
        }
        MemoryCache.savePlayerProfile(this)
    }

    private fun restoreFlag(player: Player, key: String, flag: IFlag?) {
        privateFlags.remove(key)?.cancel(player, key)
        flag?.let {
            privateFlags[key] = it
            it.init(player, key)
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

    override fun clearFlags() {
        clearFlagsAsync().exceptionally { it.printStackTrace(); null }
    }

    internal fun clearFlagsAsync(): CompletableFuture<Unit> {
        return PlayerResourceCoordinator.enqueue(uuid) {
            mainThreadFuture {
                val onlinePlayer = player
                val snapshot = privateFlags.toMap()
                snapshot.forEach { (key, flag) -> flag.cancel(onlinePlayer, key) }
                privateFlags.clear()
                onlinePlayer to snapshot
            }.thenCompose { (onlinePlayer, snapshot) ->
                val persistence = try {
                    PersistenceManager.saveProfile(createPO(), invalidate = false)
                } catch (throwable: Throwable) {
                    CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
                }
                persistence.handle { _, throwable -> throwable }
                    .thenComposeMain { throwable ->
                        if (throwable == null || throwable.databaseCommitted()) {
                            throwable?.printStackTrace()
                            CompletableFuture.completedFuture(Unit)
                        } else {
                            snapshot.forEach { (key, flag) ->
                                privateFlags[key] = flag
                                flag.init(onlinePlayer, key)
                            }
                            MemoryCache.savePlayerProfile(this)
                            CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
                        }
                    }
            }
        }
    }

    override fun givePoint(point: Int) {
        givePointAsync(point).exceptionally { it.printStackTrace(); false }
    }

    internal fun givePointAsync(point: Int): CompletableFuture<Boolean> {
        return mutatePoint { point.toLong() }
    }

    override fun takePoint(point: Int) {
        takePointAsync(point).exceptionally { it.printStackTrace(); false }
    }

    internal fun takePointAsync(point: Int): CompletableFuture<Boolean> {
        return mutatePoint { -point.toLong() }
    }

    override fun setPoint(point: Int) {
        setPointAsync(point).exceptionally { it.printStackTrace(); false }
    }

    internal fun setPointAsync(point: Int): CompletableFuture<Boolean> {
        val target = point.coerceAtLeast(0)
        return mutatePoint { current -> target.toLong() - current.toLong() }
    }

    private fun mutatePoint(delta: (Int) -> Long): CompletableFuture<Boolean> {
        return PlayerResourceCoordinator.enqueue(uuid) {
            mainThreadFuture {
                val onlinePlayer = player
                val previous = privatePoint
                val requestedDelta = delta(previous)
                if (requestedDelta == 0L) return@mainThreadFuture null
                if (requestedDelta > 0L) {
                    val requested = requestedDelta.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val event = OrryxPlayerPointEvents.Up.Pre(onlinePlayer, this, requested)
                    if (!event.call()) return@mainThreadFuture null
                    val next = (previous.toLong() + event.point.toLong().coerceAtLeast(0L))
                        .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    privatePoint = next
                    PointMutation(onlinePlayer, previous, next - previous)
                } else {
                    val requested = (-requestedDelta).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val event = OrryxPlayerPointEvents.Down.Pre(onlinePlayer, this, requested)
                    if (!event.call()) return@mainThreadFuture null
                    val next = (previous.toLong() - event.point.toLong().coerceAtLeast(0L))
                        .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    privatePoint = next
                    PointMutation(onlinePlayer, previous, next - previous)
                }
            }.thenCompose { mutation ->
                if (mutation == null) return@thenCompose CompletableFuture.completedFuture(false)
                val persistence = try {
                    PersistenceManager.saveProfile(createPO(), invalidate = false)
                } catch (throwable: Throwable) {
                    CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
                }
                persistence.handle { _, throwable -> throwable }
                    .thenComposeMain { throwable ->
                        if (throwable == null || throwable.databaseCommitted()) {
                            throwable?.printStackTrace()
                            if (mutation.delta > 0) {
                                OrryxPlayerPointEvents.Up.Post(mutation.player, this, mutation.delta).call()
                            } else {
                                OrryxPlayerPointEvents.Down.Post(mutation.player, this, -mutation.delta).call()
                            }
                            CompletableFuture.completedFuture(true)
                        } else {
                            privatePoint = mutation.previous
                            MemoryCache.savePlayerProfile(this)
                            CompletableFuture<Boolean>().also { it.completeExceptionally(throwable) }
                        }
                    }
            }
        }
    }

    override fun setJob(job: IPlayerJob) {
        setJobAsync(job).exceptionally { it.printStackTrace(); false }
    }

    internal fun setJobAsync(job: IPlayerJob): CompletableFuture<Boolean> {
        return PlayerResourceCoordinator.enqueue(uuid) {
            mainThreadFuture {
                val onlinePlayer = player
                if (!OrryxPlayerJobChangeEvents.Pre(onlinePlayer, job).call()) {
                    return@mainThreadFuture null
                }
                val previous = privateJob
                privateJob = job.key
                try {
                    JobMutation(onlinePlayer, previous, createPO(), job.createPO(), job)
                } catch (throwable: Throwable) {
                    privateJob = previous
                    throw throwable
                }
            }.thenCompose { mutation ->
                if (mutation == null) return@thenCompose CompletableFuture.completedFuture(false)
                PersistenceManager.saveProfileAndJob(mutation.profile, mutation.jobData, invalidate = true)
                    .handle { _, throwable -> throwable }
                    .thenComposeMain { throwable ->
                        if (throwable == null || throwable.databaseCommitted()) {
                            throwable?.printStackTrace()
                            OrryxPlayerJobChangeEvents.Post(mutation.player, mutation.job).call()
                            CompletableFuture.completedFuture(true)
                        } else {
                            privateJob = mutation.previousJob
                            MemoryCache.savePlayerProfile(this)
                            CompletableFuture<Boolean>().also { it.completeExceptionally(throwable) }
                        }
                    }
            }
        }
    }

    override fun createPO(): PlayerProfilePO {
        return PlayerProfilePO(
            id,
            uuid,
            job,
            point,
            privateFlags.filter { (_, flag) -> flag.isPersistence && !flag.isTimeout() }
                .mapValues { (_, flag) -> flag.toSerializable() },
        )
    }

    override fun save(async: Boolean, remove: Boolean, callback: Runnable) {
        saveAsync(async, remove).whenComplete { context, throwable ->
            finishSaveCallback(callback, throwable) {
                OrryxPlayerProfileSaveEvents.Post(
                    context.player,
                    this@PlayerProfile,
                    context.async,
                    context.remove,
                ).call()
            }
        }
    }

    private fun saveAsync(async: Boolean, remove: Boolean): CompletableFuture<SaveContext> {
        return PlayerResourceCoordinator.enqueue(uuid) {
            mainThreadFuture {
                val onlinePlayer = player
                val event = OrryxPlayerProfileSaveEvents.Pre(onlinePlayer, this, async, remove)
                event.call()
                SaveContext(onlinePlayer, event.async, event.remove, createPO())
            }.thenCompose { context ->
                PersistenceManager.saveProfile(context.data, context.remove).thenApply { context }
            }
        }
    }

    private data class PointMutation(
        val player: Player,
        val previous: Int,
        val delta: Int,
    )

    private data class JobMutation(
        val player: Player,
        val previousJob: String?,
        val profile: PlayerProfilePO,
        val jobData: org.gitee.orryx.dao.pojo.PlayerJobPO,
        val job: IPlayerJob,
    )

    private data class FlagMutation(
        val player: Player,
        val originalKey: String,
        val targetKey: String,
        val originalFlag: IFlag?,
        val targetFlag: IFlag?,
        val reportedOldFlag: IFlag?,
        val newFlag: IFlag?,
        val save: Boolean,
    )

    private data class SaveContext(
        val player: Player,
        val async: Boolean,
        val remove: Boolean,
        val data: PlayerProfilePO,
    )
}

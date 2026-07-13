package org.gitee.orryx.dao.persistence

import org.gitee.orryx.dao.cache.ISyncCacheManager
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.dao.cache.MemoryCache
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException

/** 数据库已经提交、但后续缓存发布失败。 */
internal class PersistenceWriteException(
    val databaseCommitted: Boolean,
    cause: Throwable,
) : RuntimeException(cause.message, cause)

/**
 * Orryx 持久化写入入口。
 *
 * 固定发布顺序为数据库 -> 跨服缓存 -> 本地缓存。数据库失败时不会发布较新的缓存状态；
 * 同一玩家的写入通过 [PlayerWriteCoordinator] 串行化，相同业务键尚未开始时只保留最新快照。
 */
object PersistenceManager {

    private val globalLock = Any()
    private val globalTails = ConcurrentHashMap<String, CompletableFuture<Unit>>()
    private var acceptingGlobalWrites = true
    private var globalFailure: Throwable? = null

    fun saveProfile(profile: PlayerProfilePO, invalidate: Boolean): CompletableFuture<Unit> {
        return enqueue(profile.player, PROFILE_KEY) {
            IStorageManager.INSTANCE.savePlayerDataAsync(profile).thenCompose {
                afterDatabaseCommit {
                    val cache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerProfileAsync(profile.player)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerProfileAsync(profile.player, profile)
                    }
                    cache.thenApply {
                        if (invalidate) MemoryCache.removePlayerProfile(profile.player)
                        Unit
                    }
                }
            }
        }
    }

    fun saveJob(job: PlayerJobPO, invalidate: Boolean): CompletableFuture<Unit> {
        val key = "job:${job.job.uppercase()}"
        return enqueue(job.player, key) {
            IStorageManager.INSTANCE.savePlayerJobAsync(job).thenCompose {
                afterDatabaseCommit {
                    val cache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerJobAsync(job.player, job.id, job.job)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerJobAsync(job.player, job)
                    }
                    cache.thenApply {
                        if (invalidate) MemoryCache.removePlayerJob(job.player, job.id, job.job)
                        Unit
                    }
                }
            }
        }
    }

    fun saveSkill(skill: PlayerSkillPO, invalidate: Boolean): CompletableFuture<Unit> {
        val key = "skill:${skill.job.uppercase()}:${skill.skill.uppercase()}"
        return enqueue(skill.player, key) {
            IStorageManager.INSTANCE.savePlayerSkillAsync(skill).thenCompose {
                afterDatabaseCommit {
                    val cache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerSkillAsync(skill.player, skill.id, skill.job, skill.skill)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerSkillAsync(skill.player, skill)
                    }
                    cache.thenApply {
                        if (invalidate) MemoryCache.removePlayerSkill(skill.player, skill.id, skill.job, skill.skill)
                        Unit
                    }
                }
            }
        }
    }

    fun saveKey(setting: PlayerKeySettingPO, invalidate: Boolean): CompletableFuture<Unit> {
        return enqueue(setting.player, KEY_SETTING_KEY) {
            IStorageManager.INSTANCE.savePlayerKeyAsync(setting).thenCompose {
                afterDatabaseCommit {
                    val cache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerKeySettingAsync(setting.player)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerKeySettingAsync(setting.player, setting)
                    }
                    cache.thenApply {
                        if (invalidate) MemoryCache.removePlayerKeySetting(setting.player)
                        Unit
                    }
                }
            }
        }
    }

    fun saveProfileAndJob(
        profile: PlayerProfilePO,
        job: PlayerJobPO,
        invalidate: Boolean,
    ): CompletableFuture<Unit> {
        return enqueue(profile.player, "$PROFILE_JOB_KEY:${job.job.uppercase()}") {
            IStorageManager.INSTANCE.savePlayerDataAndJobAsync(profile, job).thenCompose {
                afterDatabaseCommit {
                    val profileCache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerProfileAsync(profile.player)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerProfileAsync(profile.player, profile)
                    }
                    val jobCache = if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerJobAsync(job.player, job.id, job.job)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerJobAsync(job.player, job)
                    }
                    CompletableFuture.allOf(profileCache, jobCache).thenApply {
                        if (invalidate) {
                            MemoryCache.removePlayerProfile(profile.player)
                            MemoryCache.removePlayerJob(job.player, job.id, job.job)
                        }
                        Unit
                    }
                }
            }
        }
    }

    fun saveJobAndSkills(
        job: PlayerJobPO,
        skills: List<PlayerSkillPO>,
        invalidate: Boolean,
    ): CompletableFuture<Unit> {
        return enqueue(job.player, "job-skills:${job.job.uppercase()}") {
            IStorageManager.INSTANCE.saveJobAndSkillsAsync(job, skills).thenCompose {
                afterDatabaseCommit {
                    val cacheFutures = ArrayList<CompletableFuture<Unit>>(skills.size + 1)
                    cacheFutures += if (invalidate) {
                        ISyncCacheManager.INSTANCE.removePlayerJobAsync(job.player, job.id, job.job)
                    } else {
                        ISyncCacheManager.INSTANCE.savePlayerJobAsync(job.player, job)
                    }
                    skills.forEach { skill ->
                        cacheFutures += if (invalidate) {
                            ISyncCacheManager.INSTANCE.removePlayerSkillAsync(skill.player, skill.id, skill.job, skill.skill)
                        } else {
                            ISyncCacheManager.INSTANCE.savePlayerSkillAsync(skill.player, skill)
                        }
                    }
                    CompletableFuture.allOf(*cacheFutures.toTypedArray()).thenApply {
                        if (invalidate) {
                            MemoryCache.removePlayerJob(job.player, job.id, job.job)
                            skills.forEach { skill ->
                                MemoryCache.removePlayerSkill(skill.player, skill.id, skill.job, skill.skill)
                            }
                        }
                        Unit
                    }
                }
            }
        }
    }

    fun enqueue(
        player: UUID,
        key: String,
        operation: () -> CompletableFuture<Unit>,
    ): CompletableFuture<Unit> {
        return PlayerWriteCoordinator.enqueue(player, key, operation)
    }

    fun saveGlobalFlag(key: String, flag: IFlag?): CompletableFuture<Unit> {
        val normalized = key.lowercase()
        lateinit var result: CompletableFuture<Unit>
        lateinit var tail: CompletableFuture<Unit>
        synchronized(globalLock) {
            if (!acceptingGlobalWrites) {
                return CompletableFuture<Unit>().also {
                    it.completeExceptionally(IllegalStateException("Orryx 持久化服务正在关闭"))
                }
            }
            val ready = globalTails[normalized]?.handle { _, _ -> Unit }
                ?: CompletableFuture.completedFuture(Unit)
            result = ready.thenCompose { IStorageManager.INSTANCE.saveGlobalFlagAsync(key, flag) }
            tail = result.handle { _, throwable ->
                if (throwable != null) synchronized(globalLock) {
                    if (globalFailure == null) globalFailure = throwable
                }
                Unit
            }
            globalTails[normalized] = tail
        }
        tail.whenComplete { _, _ -> globalTails.remove(normalized, tail) }
        return result
    }

    fun flush(player: UUID): CompletableFuture<Unit> = PlayerWriteCoordinator.flush(player)

    fun release(player: UUID): CompletableFuture<Unit> = PlayerWriteCoordinator.release(player)

    fun shutdown(): CompletableFuture<Unit> {
        val globalWrites = synchronized(globalLock) {
            acceptingGlobalWrites = false
            globalTails.values.toTypedArray()
        }
        val globalDrain = CompletableFuture.allOf(*globalWrites).thenCompose {
            val failure = synchronized(globalLock) { globalFailure }
            if (failure == null) {
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture<Unit>().also { it.completeExceptionally(failure) }
            }
        }
        return CompletableFuture.allOf(PlayerWriteCoordinator.shutdown(), globalDrain).thenApply { Unit }
    }

    private fun afterDatabaseCommit(operation: () -> CompletableFuture<Unit>): CompletableFuture<Unit> {
        val future = try {
            operation()
        } catch (throwable: Throwable) {
            CompletableFuture<Unit>().also { it.completeExceptionally(throwable) }
        }
        return future.handle { _, throwable ->
            if (throwable != null) {
                throw PersistenceWriteException(databaseCommitted = true, unwrap(throwable))
            }
            Unit
        }
    }

    private fun unwrap(throwable: Throwable): Throwable {
        var current = throwable
        while (current is CompletionException || current is ExecutionException) {
            val cause = current.cause ?: break
            current = cause
        }
        return current
    }

    private const val PROFILE_KEY = "profile"
    private const val KEY_SETTING_KEY = "key-setting"
    private const val PROFILE_JOB_KEY = "profile-job"
}

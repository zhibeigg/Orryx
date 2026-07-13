package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.github.benmanes.caffeine.cache.stats.CacheStats
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.gitee.orryx.api.Orryx
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.common.keyregister.PlayerKeySetting
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.profile.PlayerProfile
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.PlayerSkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.*
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common5.format
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal fun resizeCachePolicy(
    cache: AsyncLoadingCache<*, *>,
    maximum: Long,
    expireAfterAccessMinutes: Long
) {
    cache.synchronous().policy().eviction().ifPresent { it.maximum = maximum.coerceAtLeast(1L) }
    cache.synchronous().policy().expireAfterAccess().ifPresent {
        it.setExpiresAfter(expireAfterAccessMinutes.coerceAtLeast(1L), TimeUnit.MINUTES)
    }
}

/**
 * 本服缓存，用于快速读取数据
 * */
object MemoryCache {

    private fun maximum(name: String, default: Long): Long {
        return Orryx.config.getLong("MemoryCache.$name.MaximumSize", default).coerceAtLeast(1L)
    }

    private fun expireMinutes(name: String, default: Long): Long {
        return Orryx.config.getLong("MemoryCache.$name.ExpireAfterAccessMinutes", default).coerceAtLeast(1L)
    }

    private val playerProfileCache: AsyncLoadingCache<UUID, IPlayerProfile> = Caffeine.newBuilder()
        .initialCapacity(60)
        .maximumSize(maximum("Profile", 1_000))
        .expireAfterAccess(expireMinutes("Profile", 30), TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { uuid, _ ->
            debug { "Cache 加载玩家 Profile" }
            OrryxAPI.ioScope.future {
                val po = ISyncCacheManager.INSTANCE.getPlayerProfile(uuid).await()
                val list = po.flags.mapNotNull { (key, value) ->
                    value.toFlag()?.takeUnless { it.isTimeout() }?.let { flag -> kotlin.Pair(key, flag) }
                }
                PlayerProfile(po.id, uuid, po.job, po.point, list.toMap(ConcurrentHashMap(list.size)))
            }
        }

    private val playerJobCache: AsyncLoadingCache<String, Optional<IPlayerJob>> = Caffeine.newBuilder()
        .initialCapacity(60)
        .maximumSize(maximum("Job", 1_000))
        .expireAfterAccess(expireMinutes("Job", 30), TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            debug { "Cache 加载玩家 Job" }
            OrryxAPI.ioScope.future {
                val info = reversePlayerJobDataTag(tag)
                val po = ISyncCacheManager.INSTANCE.getPlayerJob(info.second, info.third, info.first)
                Optional.ofNullable(po.await()?.let { p ->
                    PlayerJob(
                        p.id,
                        info.second,
                        p.job,
                        p.experience,
                        p.group,
                        bindKeyOfGroupToMutableMap(p.bindKeyOfGroup)
                    )
                })
            }
        }

    private val playerSkillCache: AsyncLoadingCache<String, Optional<IPlayerSkill>> = Caffeine.newBuilder()
        .initialCapacity(300)
        .maximumSize(maximum("Skill", 5_000))
        .expireAfterAccess(expireMinutes("Skill", 20), TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            debug { "Cache 加载玩家 Skill" }
            OrryxAPI.ioScope.future {
                val info = reversePlayerJobSkillDataTag(tag)
                val po = ISyncCacheManager.INSTANCE.getPlayerSkill(info.player, info.id, info.job, info.skill).await()
                Optional.ofNullable(po?.let { p ->
                    val skillLoader = SkillLoaderManager.getSkillLoader(p.skill) ?: return@let null
                    PlayerSkill(
                        p.id,
                        p.player,
                        p.skill,
                        p.job,
                        p.level,
                        if (p.locked && !skillLoader.isLocked) false else p.locked
                    )
                })
            }
        }


    private val playerKeyCache: AsyncLoadingCache<UUID, PlayerKeySetting> = Caffeine.newBuilder()
        .initialCapacity(60)
        .maximumSize(maximum("Key", 1_000))
        .expireAfterAccess(expireMinutes("Key", 30), TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { uuid, _ ->
            debug { "Cache 加载玩家 KeySetting" }
            OrryxAPI.ioScope.future {
                val profile = playerProfileCache.get(uuid).await()
                val po = ISyncCacheManager.INSTANCE.getPlayerKeySetting(uuid, profile.id).await()
                po?.let { p -> PlayerKeySetting(uuid, p) } ?: PlayerKeySetting(profile.id, uuid)
            }
        }

    @Reload(1)
    private fun resize() {
        fun resize(cache: AsyncLoadingCache<*, *>, key: String, defaultMaximum: Long, defaultExpiry: Long) {
            resizeCachePolicy(cache, maximum(key, defaultMaximum), expireMinutes(key, defaultExpiry))
        }
        resize(playerProfileCache, "Profile", 1_000, 30)
        resize(playerJobCache, "Job", 1_000, 30)
        resize(playerSkillCache, "Skill", 5_000, 20)
        resize(playerKeyCache, "Key", 1_000, 30)
    }

    fun printStats() {
        fun printStats(name: String, size: Long, stats: CacheStats) {
            consoleMessage("&e┣&f缓存：$name &7大小：$size &c命中率：${(stats.hitRate()*100).format(2)} % &c驱逐：${stats.evictionCount()} &c加载平均：${(stats.averageLoadPenalty()/1_000_000).format(2)} ms")
        }
        printStats("玩家", playerProfileCache.synchronous().estimatedSize(), playerProfileCache.synchronous().stats())
        printStats("职业", playerJobCache.synchronous().estimatedSize(), playerJobCache.synchronous().stats())
        printStats("技能", playerSkillCache.synchronous().estimatedSize(), playerSkillCache.synchronous().stats())
        printStats("按键", playerKeyCache.synchronous().estimatedSize(), playerKeyCache.synchronous().stats())
    }

    fun invalidatePlayerSkills() {
        playerSkillCache.synchronous().invalidateAll()
    }

    fun getPlayerProfile(player: UUID): CompletableFuture<IPlayerProfile> {
        return playerProfileCache.get(player)
    }

    fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<IPlayerJob?> {
        return playerJobCache.get(playerJobDataTag(player, id, job)).thenApply { it.orElse(null) }
    }

    fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<IPlayerSkill?> {
        return playerSkillCache.get(playerJobSkillDataTag(player, id, job, skill)).thenApply { it.orElse(null) }
    }

    fun getPlayerKey(player: UUID): CompletableFuture<PlayerKeySetting> {
        return playerKeyCache.get(player)
    }

    fun savePlayerProfile(playerProfile: IPlayerProfile) {
        debug { "Cache 保存玩家 Profile" }
        val previous = playerProfileCache.synchronous().getIfPresent(playerProfile.uuid)
        if (previous == null || previous.id != playerProfile.id) {
            invalidatePlayerAssociations(playerProfile.uuid)
        }
        playerProfileCache.put(playerProfile.uuid, CompletableFuture.completedFuture(playerProfile))
    }

    fun savePlayerJob(playerJob: IPlayerJob) {
        debug { "Cache 保存玩家 Job" }
        playerJobCache.put(playerJobDataTag(playerJob.uuid, playerJob.id, playerJob.key), CompletableFuture.completedFuture(Optional.of(playerJob)))
    }

    fun savePlayerSkill(playerSkill: IPlayerSkill) {
        debug { "Cache 保存玩家 Skill" }
        playerSkillCache.put(playerJobSkillDataTag(playerSkill.uuid, playerSkill.id, playerSkill.job, playerSkill.key), CompletableFuture.completedFuture(Optional.of(playerSkill)))
    }

    fun savePlayerKeySetting(player: UUID, setting: PlayerKeySetting) {
        debug { "Cache 保存玩家 KeySetting" }
        playerKeyCache.put(player, CompletableFuture.completedFuture(setting))
    }

    fun removePlayerProfile(player: UUID) {
        debug { "Cache 移除玩家 Profile" }
        playerProfileCache.synchronous().invalidate(player)
        invalidatePlayerAssociations(player)
    }

    private fun invalidatePlayerAssociations(player: UUID) {
        val prefix = playerDataTag(player).removeSuffix(":profile")
        playerKeyCache.synchronous().invalidate(player)
        playerJobCache.synchronous().invalidateAll(
            playerJobCache.synchronous().asMap().keys.filter { it.startsWith("$prefix:job:") }
        )
        playerSkillCache.synchronous().invalidateAll(
            playerSkillCache.synchronous().asMap().keys.filter { it.startsWith("$prefix:skill:") }
        )
    }

    fun removePlayerJob(player: UUID, id: Int, job: String) {
        debug { "Cache 移除玩家 Job" }
        playerJobCache.synchronous().invalidate(playerJobDataTag(player, id, job))
    }

    fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String) {
        debug { "Cache 移除玩家 Skill" }
        playerSkillCache.synchronous().invalidate(playerJobSkillDataTag(player, id, job, skill))
    }

    fun removePlayerKeySetting(player: UUID) {
        debug { "Cache 移除玩家 KeySetting" }
        playerKeyCache.synchronous().invalidate(player)
    }
}
package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.github.benmanes.caffeine.cache.stats.CacheStats
import org.bukkit.Bukkit
import org.bukkit.entity.Player
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
import taboolib.common.platform.function.info
import taboolib.common5.format
import taboolib.module.chat.colored
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 本服缓存，用于快速读取数据
 * */
object MemoryCache {

    private val playerProfileCache: AsyncLoadingCache<UUID, IPlayerProfile> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { uuid, _ ->
            val po = ISyncCacheManager.INSTANCE.getPlayerProfile(uuid)
            po.thenApply {
                it?.let { p ->
                    val list = p.flags.mapNotNull { (key, value) ->
                        value.toFlag()?.let { flag -> key to flag }
                    }
                    PlayerProfile(Bukkit.getPlayer(uuid) ?: return@let null, p.job, p.point, list.toMap(ConcurrentHashMap(list.size)))
                }?: PlayerProfile(Bukkit.getPlayer(uuid) ?: return@thenApply null, null, 0, ConcurrentHashMap())
            }
        }

    private val playerJobCache: AsyncLoadingCache<String, IPlayerJob> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            val info = reversePlayerJobDataTag(tag)
            val po = ISyncCacheManager.INSTANCE.getPlayerJob(info.second, info.first)
            po.thenApply {
                it?.let { p ->
                    Bukkit.getPlayer(info.second)?.let { player ->
                        PlayerJob(player, p.job, p.experience, p.group, bindKeyOfGroupToMutableMap(p.bindKeyOfGroup))
                    }
                }
            }
        }

    private val playerSkillCache: AsyncLoadingCache<String, IPlayerSkill> = Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            val info = reversePlayerJobSkillDataTag(tag)
            ISyncCacheManager.INSTANCE.getPlayerSkill(info.player, info.job, info.skill).thenApply {
                it?.let { p ->
                    val skillLoader = SkillLoaderManager.getSkillLoader(p.skill) ?: return@let null
                    PlayerSkill(
                        Bukkit.getPlayer(p.player) ?: return@let null,
                        p.skill,
                        p.job,
                        p.level,
                        if (p.locked && !skillLoader.isLocked) false else p.locked
                    )
                }
            }
        }

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        fun printStats(name: String, stats: CacheStats) {
            info("&e┣&f缓存：$name &c命中率：${(stats.hitRate()*100).format(2)} % &c加载平均时间：${stats.averageLoadPenalty()/1000000} ms".colored())
        }
        printStats("玩家", playerProfileCache.synchronous().stats())
        printStats("职业", playerJobCache.synchronous().stats())
        printStats("技能", playerSkillCache.synchronous().stats())
    }

    fun getPlayerProfile(player: Player): CompletableFuture<IPlayerProfile> {
        return playerProfileCache.get(player.uniqueId)
    }

    fun getPlayerJob(player: Player, job: String): CompletableFuture<IPlayerJob?> {
        return playerJobCache.get(playerJobDataTag(player.uniqueId, job))
    }

    fun getPlayerSkill(player: Player, job: String, skill: String): CompletableFuture<IPlayerSkill?> {
        return playerSkillCache.get(playerJobSkillDataTag(player.uniqueId, job, skill))
    }

    fun savePlayerProfile(playerProfile: IPlayerProfile) {
        playerProfileCache.put(playerProfile.player.uniqueId, CompletableFuture.completedFuture(playerProfile))
    }

    fun savePlayerJob(playerJob: IPlayerJob) {
        playerJobCache.put(playerJobDataTag(playerJob.player.uniqueId, playerJob.key), CompletableFuture.completedFuture(playerJob))
    }

    fun savePlayerSkill(playerSkill: IPlayerSkill) {
        playerSkillCache.put(playerJobSkillDataTag(playerSkill.player.uniqueId, playerSkill.job, playerSkill.key), CompletableFuture.completedFuture(playerSkill))
    }

    fun removePlayerProfile(player: UUID) {
        playerProfileCache.synchronous().invalidate(player)
    }

    fun removePlayerJob(player: UUID, job: String) {
        playerProfileCache.synchronous().invalidate(playerJobDataTag(player, job))
    }

    fun removePlayerSkill(player: UUID, job: String, skill: String) {
        playerProfileCache.synchronous().invalidate(playerJobSkillDataTag(player, job, skill))
    }

}
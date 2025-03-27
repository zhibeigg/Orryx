package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.github.benmanes.caffeine.cache.stats.CacheStats
import org.bukkit.entity.Player
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.profile.PlayerProfile
import org.gitee.orryx.core.skill.IPlayerSkill
import org.gitee.orryx.core.skill.PlayerSkill
import org.gitee.orryx.core.skill.SkillLoaderManager
import org.gitee.orryx.utils.bindKeyOfGroupToMutableMap
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import org.gitee.orryx.utils.toFlag
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common5.format
import taboolib.module.chat.colored
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 本服缓存，用于快速读取数据
 * */
object MemoryCache {

    private val playerProfileCache: Cache<UUID, IPlayerProfile> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build()

    private val playerJobCache: Cache<String, IPlayerJob> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build()

    private val playerSkillCache: Cache<String, IPlayerSkill> = Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build()

    @Awake(LifeCycle.DISABLE)
    private fun disable() {
        fun printStats(name: String, stats: CacheStats) {
            info("&e┣&f缓存：$name &c命中率：${(stats.hitRate()*100).format(2)} % &c加载平均时间：${stats.averageLoadPenalty()/1000000} ms".colored())
        }
        printStats("玩家", playerProfileCache.stats())
        printStats("职业", playerJobCache.stats())
        printStats("技能", playerSkillCache.stats())
    }

    fun getPlayerProfile(player: Player): IPlayerProfile {
        return playerProfileCache.get(player.uniqueId) {
            val po = ISyncCacheManager.INSTANCE.getPlayerData(it)
            po?.let { p ->
                val list = p.flags.mapNotNull { (key, value) ->
                    value.toFlag()?.let { flag -> key to flag }
                }
                PlayerProfile(player, p.job, p.point, list.toMap(ConcurrentHashMap(list.size)))
            }
        } ?: PlayerProfile(player, null, 0, ConcurrentHashMap())
    }

    fun getPlayerJob(player: Player, job: String): IPlayerJob? {
        return playerJobCache.get(playerJobDataTag(player.uniqueId, job)) {
            val po = ISyncCacheManager.INSTANCE.getPlayerJob(player.uniqueId, job)
            po?.let { p ->
                PlayerJob(player, p.job, p.experience, p.group, bindKeyOfGroupToMutableMap(p.bindKeyOfGroup))
            }
        }
    }

    fun getPlayerSkill(player: Player, job: String, skill: String): IPlayerSkill? {
        return playerSkillCache.get(playerJobSkillDataTag(player.uniqueId, job, skill)) {
            val po = ISyncCacheManager.INSTANCE.getPlayerSkill(player.uniqueId, job, skill)
            po?.let { p ->
                val skillLoader = SkillLoaderManager.getSkillLoader(skill) ?: return@let null
                PlayerSkill(player, skill, job, p.level, if (p.locked && !skillLoader.isLocked) false else p.locked)
            }
        }
    }

    fun savePlayerProfile(playerProfile: PlayerProfile) {
        playerProfileCache.put(playerProfile.player.uniqueId, playerProfile)
    }

    fun savePlayerJob(playerJob: PlayerJob) {
        playerJobCache.put(playerJobDataTag(playerJob.player.uniqueId, playerJob.key), playerJob)
    }

    fun savePlayerSkill(playerSkill: PlayerSkill) {
        playerSkillCache.put(playerJobSkillDataTag(playerSkill.player.uniqueId, playerSkill.job, playerSkill.key), playerSkill)
    }

    fun removePlayerProfile(player: UUID) {
        playerProfileCache.invalidate(player)
    }

    fun removePlayerJob(player: UUID, job: String) {
        playerProfileCache.invalidate(playerJobDataTag(player, job))
    }

    fun removePlayerSkill(player: UUID, job: String, skill: String) {
        playerProfileCache.invalidate(playerJobSkillDataTag(player, job, skill))
    }

}
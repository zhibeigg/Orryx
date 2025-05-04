package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.github.benmanes.caffeine.cache.stats.CacheStats
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
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
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
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
        .initialCapacity(60)
        .maximumSize(100)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { uuid, _ ->
            debug("Cache 加载玩家 Profile")
            val po = ISyncCacheManager.INSTANCE.getPlayerProfile(uuid)
            po.thenApply {
                it?.let { p ->
                    val list = p.flags.mapNotNull { (key, value) ->
                        value.toFlag()?.let { flag -> key to flag }
                    }
                    PlayerProfile(Bukkit.getPlayer(uuid) ?: return@let null, p.job, p.point, list.toMap(ConcurrentHashMap(list.size)))
                } ?: PlayerProfile(Bukkit.getPlayer(uuid) ?: return@thenApply null, null, 0, ConcurrentHashMap())
            }
        }

    private val playerJobCache: AsyncLoadingCache<String, IPlayerJob> = Caffeine.newBuilder()
        .initialCapacity(60)
        .maximumSize(100)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            debug("Cache 加载玩家 Job")
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
        .initialCapacity(300)
        .maximumSize(500)
        .expireAfterAccess(20, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { tag, _ ->
            debug("Cache 加载玩家 Skill")
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


    private val playerKeyCache: AsyncLoadingCache<UUID, PlayerKeySetting> = Caffeine.newBuilder()
        .initialCapacity(60)
        .maximumSize(100)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .buildAsync { uuid, _ ->
            debug("Cache 加载玩家 KeySetting")
            ISyncCacheManager.INSTANCE.getPlayerKeySetting(uuid).thenApply {
                val player = Bukkit.getPlayer(uuid)?: return@thenApply null
                it?.let { p -> PlayerKeySetting(player, p)} ?: PlayerKeySetting(player)
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
        printStats("按键", playerKeyCache.synchronous().stats())
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

    fun getPlayerKey(player: Player): CompletableFuture<PlayerKeySetting> {
        return playerKeyCache.get(player.uniqueId)
    }

    fun savePlayerProfile(playerProfile: IPlayerProfile) {
        debug("Cache 保存玩家 Profile")
        playerProfileCache.put(playerProfile.player.uniqueId, CompletableFuture.completedFuture(playerProfile))
    }

    fun savePlayerJob(playerJob: IPlayerJob) {
        debug("Cache 保存玩家 Job")
        playerJobCache.put(playerJobDataTag(playerJob.player.uniqueId, playerJob.key), CompletableFuture.completedFuture(playerJob))
    }

    fun savePlayerSkill(playerSkill: IPlayerSkill) {
        debug("Cache 保存玩家 Skill")
        playerSkillCache.put(playerJobSkillDataTag(playerSkill.player.uniqueId, playerSkill.job, playerSkill.key), CompletableFuture.completedFuture(playerSkill))
    }

    fun savePlayerKeySetting(player: UUID, setting: PlayerKeySetting) {
        debug("Cache 保存玩家 KeySetting")
        playerKeyCache.put(player, CompletableFuture.completedFuture(setting))
    }

    fun removePlayerProfile(player: UUID) {
        debug("Cache 移除玩家 Profile")
        playerProfileCache.synchronous().invalidate(player)
    }

    fun removePlayerJob(player: UUID, job: String) {
        debug("Cache 移除玩家 Job")
        playerJobCache.synchronous().invalidate(playerJobDataTag(player, job))
    }

    fun removePlayerSkill(player: UUID, job: String, skill: String) {
        debug("Cache 移除玩家 Skill")
        playerSkillCache.synchronous().invalidate(playerJobSkillDataTag(player, job, skill))
    }

    fun removePlayerKeySetting(player: UUID) {
        debug("Cache 移除玩家 KeySetting")
        playerKeyCache.synchronous().invalidate(player)
    }
}
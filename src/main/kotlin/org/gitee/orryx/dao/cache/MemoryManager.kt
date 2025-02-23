package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.Scheduler
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import org.gitee.orryx.utils.reversePlayerJobDataTag
import org.gitee.orryx.utils.reversePlayerJobSkillDataTag
import java.util.*
import java.util.concurrent.TimeUnit

class MemoryManager: ICacheManager {

    internal val playerDataCache: LoadingCache<UUID, PlayerData> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            IStorageManager.INSTANCE.getPlayerData(key)
        }

    internal val playerJobCache: LoadingCache<String, PlayerJob> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            val list = reversePlayerJobDataTag(key)
            IStorageManager.INSTANCE.getPlayerJob(UUID.fromString(list[0]), list[1])
        }

    internal val playerSkillCache: LoadingCache<String, PlayerSkill> = Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            val list = reversePlayerJobSkillDataTag(key)
            IStorageManager.INSTANCE.getPlayerSkill(UUID.fromString(list[0]), list[1], list[2])
        }

    override fun getPlayerData(player: UUID): PlayerData? {
        return playerDataCache.get(player) {
            IStorageManager.INSTANCE.getPlayerData(player)
        }
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJob? {
        return playerJobCache.get(playerJobDataTag(player, job)) {
            IStorageManager.INSTANCE.getPlayerJob(player, job)
        }
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill? {
        return playerSkillCache.get(playerJobSkillDataTag(player, job, skill)) {
            IStorageManager.INSTANCE.getPlayerSkill(player, job, skill)
        }
    }

    override fun savePlayerData(player: UUID, playerData: PlayerData, async: Boolean) {
        playerDataCache.put(player, playerData)
    }

    override fun savePlayerJob(player: UUID, playerJob: PlayerJob, async: Boolean) {
        playerJobCache.put(playerJobDataTag(player, playerJob.job), playerJob)
    }

    override fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill, async: Boolean) {
        playerSkillCache.put(playerJobSkillDataTag(player, playerSkill.job, playerSkill.skill), playerSkill)
    }

    override fun removePlayerData(player: UUID, async: Boolean) {
        playerDataCache.invalidate(player)
    }

    override fun removePlayerJob(player: UUID, job: String, async: Boolean) {
        playerDataCache.invalidate(playerJobDataTag(player, job))
    }

    override fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean) {
        playerDataCache.invalidate(playerJobSkillDataTag(player, job, skill))
    }

}
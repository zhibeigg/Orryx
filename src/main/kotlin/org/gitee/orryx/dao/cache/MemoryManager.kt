package org.gitee.orryx.dao.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.Scheduler
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import org.gitee.orryx.utils.reversePlayerJobDataTag
import org.gitee.orryx.utils.reversePlayerJobSkillDataTag
import java.util.*
import java.util.concurrent.TimeUnit

class MemoryManager: ICacheManager {

    internal val playerProfilePOCache: LoadingCache<UUID, PlayerProfilePO> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            IStorageManager.INSTANCE.getPlayerData(key)
        }

    internal val playerJobPOCache: LoadingCache<String, PlayerJobPO> = Caffeine.newBuilder()
        .initialCapacity(20)
        .maximumSize(100)
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            val list = reversePlayerJobDataTag(key)
            IStorageManager.INSTANCE.getPlayerJob(UUID.fromString(list[0]), list[1])
        }

    internal val playerSkillPOCache: LoadingCache<String, PlayerSkillPO> = Caffeine.newBuilder()
        .initialCapacity(100)
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .recordStats()
        .scheduler(Scheduler.systemScheduler())
        .build { key ->
            val list = reversePlayerJobSkillDataTag(key)
            IStorageManager.INSTANCE.getPlayerSkill(UUID.fromString(list[0]), list[1], list[2])
        }

    override fun getPlayerData(player: UUID): PlayerProfilePO? {
        return playerProfilePOCache.get(player) {
            IStorageManager.INSTANCE.getPlayerData(player)
        }
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJobPO? {
        return playerJobPOCache.get(playerJobDataTag(player, job)) {
            IStorageManager.INSTANCE.getPlayerJob(player, job)
        }
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkillPO? {
        return playerSkillPOCache.get(playerJobSkillDataTag(player, job, skill)) {
            IStorageManager.INSTANCE.getPlayerSkill(player, job, skill)
        }
    }

    override fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO, async: Boolean) {
        playerProfilePOCache.put(player, playerProfilePO)
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
        playerJobPOCache.put(playerJobDataTag(player, playerJobPO.job), playerJobPO)
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
        playerSkillPOCache.put(playerJobSkillDataTag(player, playerSkillPO.job, playerSkillPO.skill), playerSkillPO)
    }

    override fun removePlayerData(player: UUID, async: Boolean) {
        playerProfilePOCache.invalidate(player)
    }

    override fun removePlayerJob(player: UUID, job: String, async: Boolean) {
        playerProfilePOCache.invalidate(playerJobDataTag(player, job))
    }

    override fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean) {
        playerProfilePOCache.invalidate(playerJobSkillDataTag(player, job, skill))
    }

}
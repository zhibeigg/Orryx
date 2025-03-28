package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.playerDataTag
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.unsafeLazy
import java.util.*
import java.util.concurrent.CompletableFuture

class RedisManager: ISyncCacheManager {

    private val api by unsafeLazy { RedisChannelPlugin.api }

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO?> {
        val tag = playerDataTag(player)
        val future = CompletableFuture<PlayerProfilePO?>()
        fun read() {
            try {
                var playerProfilePO = api.get(playerDataTag(player))?.let { Json.decodeFromString<PlayerProfilePO>(it) }
                if (playerProfilePO == null) {
                    playerProfilePO = IStorageManager.INSTANCE.getPlayerData(player).join()
                    playerProfilePO?.let { savePlayerData(player, it, false) }
                } else {
                    api.refreshExpire(tag, 900, false)
                }
                future.complete(playerProfilePO)
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerJob(player: UUID, job: String): CompletableFuture<PlayerJobPO?> {
        val tag = playerJobDataTag(player, job)
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
            try {
                var playerJobPO = api.get(playerJobDataTag(player, job))?.let { Json.decodeFromString<PlayerJobPO>(it) }
                if (playerJobPO == null) {
                    playerJobPO = IStorageManager.INSTANCE.getPlayerJob(player, job).join()
                    playerJobPO?.let { savePlayerJob(player, it, false) }
                } else {
                    api.refreshExpire(tag, 900, false)
                }
                future.complete(playerJobPO)
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        val tag = playerJobSkillDataTag(player, job, skill)
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            try {
                var playerSkillPO = api.get(tag)?.let { Json.decodeFromString<PlayerSkillPO>(it) }
                if (playerSkillPO == null) {
                    playerSkillPO = IStorageManager.INSTANCE.getPlayerSkill(player, job, skill).join()
                    playerSkillPO?.let { savePlayerSkill(player, it, false) }
                } else {
                    api.refreshExpire(tag, 600, false)
                }
                future.complete(playerSkillPO)
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        if (isPrimaryThread) {
            submitAsync { read() }
        } else {
            read()
        }
        return future
    }

    override fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO, async: Boolean) {
        api.set(playerDataTag(player), Json.encodeToString(playerProfilePO), 900, async)
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
        api.set(playerJobDataTag(player, playerJobPO.job), Json.encodeToString(playerJobPO), 900, async)
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
        api.set(playerJobSkillDataTag(player, playerSkillPO.job, playerSkillPO.skill), Json.encodeToString(playerSkillPO), 600, async)
    }

    override fun removePlayerData(player: UUID, async: Boolean) {
        api.remove(playerDataTag(player), async)
    }

    override fun removePlayerJob(player: UUID, job: String, async: Boolean) {
        api.remove(playerJobDataTag(player, job), async)
    }

    override fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean) {
        api.remove(playerJobSkillDataTag(player, job, skill), async)
    }

}
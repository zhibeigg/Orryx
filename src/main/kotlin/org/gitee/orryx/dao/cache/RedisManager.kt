package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.playerDataTag
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import org.gitee.orryx.utils.playerKeySettingDataTag
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submitAsync
import taboolib.common.util.unsafeLazy
import java.util.*
import java.util.concurrent.CompletableFuture

class RedisManager: ISyncCacheManager {

    companion object {
        const val SECOND_6_HOURS = 6 * 60 * 60L
        const val SECOND_12_HOURS = 12 * 60 * 60L
    }

    private val api by unsafeLazy { RedisChannelPlugin.api }

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO?> {
        val tag = playerDataTag(player)
        val future = CompletableFuture<PlayerProfilePO?>()
        fun read() {
            try {
                var playerProfilePO = api.get(playerDataTag(player))?.let { Json.decodeFromString<PlayerProfilePO>(it) }
                if (playerProfilePO == null) {
                    playerProfilePO = IStorageManager.INSTANCE.getPlayerData(player).join()
                    playerProfilePO?.let { savePlayerProfile(player, it, false) }
                } else {
                    api.refreshExpire(tag, SECOND_12_HOURS, false)
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
                    api.refreshExpire(tag, SECOND_12_HOURS, false)
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
                    api.refreshExpire(tag, SECOND_6_HOURS, false)
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

    override fun getPlayerKeySetting(player: UUID): CompletableFuture<PlayerKeySettingPO?> {
        val tag = playerKeySettingDataTag(player)
        val future = CompletableFuture<PlayerKeySettingPO?>()
        fun read() {
            try {
                var playerKeySettingPO = api.get(tag)?.let { Json.decodeFromString<PlayerKeySettingPO>(it) }
                if (playerKeySettingPO == null) {
                    playerKeySettingPO = IStorageManager.INSTANCE.getPlayerKey(player).join()
                    playerKeySettingPO?.let { savePlayerKeySetting(player, it, false) }
                } else {
                    api.refreshExpire(tag, SECOND_6_HOURS, false)
                }
                future.complete(playerKeySettingPO)
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

    override fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO, async: Boolean) {
        api.set(playerDataTag(player), Json.encodeToString(playerProfilePO), SECOND_12_HOURS, async)
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
        api.set(playerJobDataTag(player, playerJobPO.job), Json.encodeToString(playerJobPO), SECOND_12_HOURS, async)
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
        api.set(playerJobSkillDataTag(player, playerSkillPO.job, playerSkillPO.skill), Json.encodeToString(playerSkillPO), SECOND_6_HOURS, async)
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO, async: Boolean) {
        api.set(playerKeySettingDataTag(player), Json.encodeToString(playerKeySettingPO), SECOND_6_HOURS, async)
    }

    override fun removePlayerProfile(player: UUID, async: Boolean) {
        api.remove(playerDataTag(player), async)
    }

    override fun removePlayerJob(player: UUID, job: String, async: Boolean) {
        api.remove(playerJobDataTag(player, job), async)
    }

    override fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean) {
        api.remove(playerJobSkillDataTag(player, job, skill), async)
    }

    override fun removePlayerKeySetting(player: UUID, async: Boolean) {
        api.remove(playerKeySettingDataTag(player), async)
    }

}
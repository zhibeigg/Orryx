package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import com.gitee.redischannel.util.proxyAsyncCommand
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.*
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submitAsync
import java.util.*
import java.util.concurrent.CompletableFuture

class RedisManager: ISyncCacheManager {

    companion object {
        const val SECOND_6_HOURS = 6 * 60 * 60L
        const val SECOND_12_HOURS = 12 * 60 * 60L
    }

    private val api by lazy { RedisChannelPlugin.api }

    private val asyncApi
        get() = api.proxyAsyncCommand()

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO> {
        debug("Redis 获取玩家 Profile")
        val tag = playerDataTag(player)
        val future = CompletableFuture<PlayerProfilePO>()
        fun read() {
            try {
                var playerProfilePO = api.stringCommand()[playerDataTag(player)]?.let { Json.decodeFromString<PlayerProfilePO>(it) }
                if (playerProfilePO == null) {
                    playerProfilePO = IStorageManager.INSTANCE.getPlayerData(player).join()
                    playerProfilePO?.let { savePlayerProfile(player, it, false) }
                } else {
                    api.keyCommand().expire(tag, SECOND_12_HOURS)
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

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        debug("Redis 获取玩家 Job")
        val tag = playerJobDataTag(player, id, job)
        val future = CompletableFuture<PlayerJobPO?>()
        fun read() {
            try {
                var playerJobPO = api.stringCommand()[playerJobDataTag(player, id, job)]?.let { Json.decodeFromString<PlayerJobPO>(it) }
                if (playerJobPO == null) {
                    playerJobPO = IStorageManager.INSTANCE.getPlayerJob(player, id, job).join()
                    playerJobPO?.let { savePlayerJob(player, it, false) }
                } else {
                    api.keyCommand().expire(tag, SECOND_12_HOURS)
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

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug("Redis 获取玩家 Skill")
        val tag = playerJobSkillDataTag(player, id, job, skill)
        val future = CompletableFuture<PlayerSkillPO?>()
        fun read() {
            try {
                var playerSkillPO = api.stringCommand()[tag]?.let { Json.decodeFromString<PlayerSkillPO>(it) }
                if (playerSkillPO == null) {
                    playerSkillPO = IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill).join()
                    playerSkillPO?.let { savePlayerSkill(player, it, false) }
                } else {
                    api.keyCommand().expire(tag, SECOND_6_HOURS)
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

    override fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?> {
        debug("Redis 获取玩家 KeySetting")
        val tag = playerKeySettingDataTag(player)
        val future = CompletableFuture<PlayerKeySettingPO?>()
        fun read() {
            try {
                var playerKeySettingPO = api.stringCommand()[tag]?.let { Json.decodeFromString<PlayerKeySettingPO>(it) }
                if (playerKeySettingPO == null) {
                    playerKeySettingPO = IStorageManager.INSTANCE.getPlayerKey(id).join()
                    playerKeySettingPO?.let { savePlayerKeySetting(player, it, false) }
                } else {
                    api.keyCommand().expire(tag, SECOND_6_HOURS)
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
        debug("Redis 获取玩家 Profile")
        if (async) {
            asyncApi.thenAccept {
                it.setex(playerDataTag(player), SECOND_12_HOURS, Json.encodeToString(playerProfilePO))
            }
        } else {
            api.stringCommand().setex(playerDataTag(player), SECOND_12_HOURS, Json.encodeToString(playerProfilePO))
        }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
        debug("Redis 获取玩家 Job")
        if (async) {
            asyncApi.thenAccept {
                it.setex(playerJobDataTag(player, playerJobPO.id, playerJobPO.job), SECOND_12_HOURS, Json.encodeToString(playerJobPO))
            }
        } else {
            api.stringCommand().setex(playerJobDataTag(player, playerJobPO.id, playerJobPO.job), SECOND_12_HOURS, Json.encodeToString(playerJobPO))
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
        debug("Redis 保存玩家 Skill")
        if (async) {
            asyncApi.thenAccept {
                it.setex(playerJobSkillDataTag(player, playerSkillPO.id, playerSkillPO.job, playerSkillPO.skill), SECOND_6_HOURS, Json.encodeToString(playerSkillPO))
            }
        } else {
            api.stringCommand().setex(playerJobSkillDataTag(player, playerSkillPO.id, playerSkillPO.job, playerSkillPO.skill), SECOND_6_HOURS, Json.encodeToString(playerSkillPO))
        }
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO, async: Boolean) {
        debug("Redis 保存玩家 KeySetting")
        if (async) {
            asyncApi.thenAccept {
                it.setex(playerKeySettingDataTag(player), SECOND_6_HOURS, Json.encodeToString(playerKeySettingPO))
            }
        } else {
            api.stringCommand().setex(playerKeySettingDataTag(player), SECOND_6_HOURS, Json.encodeToString(playerKeySettingPO))
        }
    }

    override fun removePlayerProfile(player: UUID, async: Boolean) {
        debug("Redis 移除玩家 Profile")
        if (async) {
            asyncApi.thenAccept {
                it.del(playerDataTag(player))
            }
        } else {
            api.keyCommand().del(playerDataTag(player))
        }
    }

    override fun removePlayerJob(player: UUID, id: Int, job: String, async: Boolean) {
        debug("Redis 移除玩家 Job")
        if (async) {
            asyncApi.thenAccept {
                it.del(playerJobDataTag(player, id, job))
            }
        } else {
            api.keyCommand().del(playerJobDataTag(player, id, job))
        }
    }

    override fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String, async: Boolean) {
        debug("Redis 移除玩家 Skill")
        if (async) {
            asyncApi.thenAccept {
                it.del(playerJobSkillDataTag(player, id, job, skill))
            }
        } else {
            api.keyCommand().del(playerJobSkillDataTag(player, id, job, skill))
        }
    }

    override fun removePlayerKeySetting(player: UUID, async: Boolean) {
        debug("Redis 移除玩家 KeySetting")
        if (async) {
            asyncApi.thenAccept {
                it.del(playerKeySettingDataTag(player))
            }
        } else {
            api.keyCommand().del(playerKeySettingDataTag(player))
        }
    }
}
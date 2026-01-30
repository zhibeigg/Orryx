package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.*
import java.util.*
import java.util.concurrent.CompletableFuture

@Suppress("DuplicatedCode")
class ClusterRedisManager: ISyncCacheManager {

    private val api by lazy { RedisChannelPlugin.clusterCommandAPI() }

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO> {
        requireAsync("redis")
        debug { "Redis 获取玩家 Profile" }
        val tag = playerDataTag(player)
        val future = CompletableFuture<PlayerProfilePO>()
        api.useAsyncCommands { commands ->
            commands[tag].whenComplete { json, error ->
                when {
                    error != null -> {
                        error.printStackTrace()
                        IStorageManager.INSTANCE.getPlayerData(player).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                savePlayerProfile(player, data)
                                future.complete(data)
                            }
                        }
                    }
                    json == null -> {
                        IStorageManager.INSTANCE.getPlayerData(player).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                savePlayerProfile(player, data)
                                future.complete(data)
                            }
                        }
                    }
                    else -> {
                        try {
                            commands.expire(tag, RedisManager.SECOND_12_HOURS)
                            future.complete(Json.decodeFromString<PlayerProfilePO>(json))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            IStorageManager.INSTANCE.getPlayerData(player).whenComplete { data, storageError ->
                                if (storageError != null) {
                                    future.completeExceptionally(storageError)
                                } else {
                                    savePlayerProfile(player, data)
                                    future.complete(data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return future
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        requireAsync("redis")
        debug { "Redis 获取玩家 Job" }
        val tag = playerJobDataTag(player, id, job)
        val future = CompletableFuture<PlayerJobPO?>()
        api.useAsyncCommands { commands ->
            commands[tag].whenComplete { json, error ->
                when {
                    error != null -> {
                        error.printStackTrace()
                        IStorageManager.INSTANCE.getPlayerJob(player, id, job).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerJob(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    json == null -> {
                        IStorageManager.INSTANCE.getPlayerJob(player, id, job).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerJob(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    else -> {
                        try {
                            commands.expire(tag, RedisManager.SECOND_12_HOURS)
                            future.complete(Json.decodeFromString<PlayerJobPO>(json))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            IStorageManager.INSTANCE.getPlayerJob(player, id, job).whenComplete { data, storageError ->
                                if (storageError != null) {
                                    future.completeExceptionally(storageError)
                                } else {
                                    data?.let { savePlayerJob(player, it) }
                                    future.complete(data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return future
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        requireAsync("redis")
        debug { "Redis 获取玩家 Skill" }
        val tag = playerJobSkillDataTag(player, id, job, skill)
        val future = CompletableFuture<PlayerSkillPO?>()
        api.useAsyncCommands { commands ->
            commands[tag].whenComplete { json, error ->
                when {
                    error != null -> {
                        error.printStackTrace()
                        IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerSkill(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    json == null -> {
                        IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerSkill(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    else -> {
                        try {
                            commands.expire(tag, RedisManager.SECOND_6_HOURS)
                            future.complete(Json.decodeFromString<PlayerSkillPO>(json))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill).whenComplete { data, storageError ->
                                if (storageError != null) {
                                    future.completeExceptionally(storageError)
                                } else {
                                    data?.let { savePlayerSkill(player, it) }
                                    future.complete(data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return future
    }

    override fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?> {
        requireAsync("redis")
        debug { "Redis 获取玩家 KeySetting" }
        val tag = playerKeySettingDataTag(player)
        val future = CompletableFuture<PlayerKeySettingPO?>()
        api.useAsyncCommands { commands ->
            commands[tag].whenComplete { json, error ->
                when {
                    error != null -> {
                        error.printStackTrace()
                        IStorageManager.INSTANCE.getPlayerKey(id).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerKeySetting(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    json == null -> {
                        IStorageManager.INSTANCE.getPlayerKey(id).whenComplete { data, storageError ->
                            if (storageError != null) {
                                future.completeExceptionally(storageError)
                            } else {
                                data?.let { savePlayerKeySetting(player, it) }
                                future.complete(data)
                            }
                        }
                    }
                    else -> {
                        try {
                            commands.expire(tag, RedisManager.SECOND_6_HOURS)
                            future.complete(Json.decodeFromString<PlayerKeySettingPO>(json))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            IStorageManager.INSTANCE.getPlayerKey(id).whenComplete { data, storageError ->
                                if (storageError != null) {
                                    future.completeExceptionally(storageError)
                                } else {
                                    data?.let { savePlayerKeySetting(player, it) }
                                    future.complete(data)
                                }
                            }
                        }
                    }
                }
            }
        }
        return future
    }

    override fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO) {
        requireAsync("redis")
        debug { "Redis 获取玩家 Profile" }
        api.useAsyncCommands { commands ->
            commands.setex(playerDataTag(player), RedisManager.SECOND_12_HOURS, Json.encodeToString(playerProfilePO))
        }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO) {
        requireAsync("redis")
        debug { "Redis 获取玩家 Job" }
        api.useAsyncCommands { commands ->
            commands.setex(playerJobDataTag(player, playerJobPO.id, playerJobPO.job),
                RedisManager.SECOND_12_HOURS, Json.encodeToString(playerJobPO))
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO) {
        requireAsync("redis")
        debug { "Redis 保存玩家 Skill" }
        api.useAsyncCommands { commands ->
            commands.setex(playerJobSkillDataTag(player, playerSkillPO.id, playerSkillPO.job, playerSkillPO.skill),
                RedisManager.SECOND_6_HOURS, Json.encodeToString(playerSkillPO))
        }
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO) {
        requireAsync("redis")
        debug { "Redis 保存玩家 KeySetting" }
        api.useAsyncCommands { commands ->
            commands.setex(playerKeySettingDataTag(player), RedisManager.SECOND_6_HOURS, Json.encodeToString(playerKeySettingPO))
        }
    }

    override fun removePlayerProfile(player: UUID) {
        requireAsync("redis")
        debug { "Redis 移除玩家 Profile" }
        api.useAsyncCommands { commands ->
            commands.del(playerDataTag(player))
        }
    }

    override fun removePlayerJob(player: UUID, id: Int, job: String) {
        requireAsync("redis")
        debug { "Redis 移除玩家 Job" }
        api.useAsyncCommands { commands ->
            commands.del(playerJobDataTag(player, id, job))
        }
    }

    override fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String) {
        requireAsync("redis")
        debug { "Redis 移除玩家 Skill" }
        api.useAsyncCommands { commands ->
            commands.del(playerJobSkillDataTag(player, id, job, skill))
        }
    }

    override fun removePlayerKeySetting(player: UUID) {
        requireAsync("redis")
        debug { "Redis 移除玩家 KeySetting" }
        api.useAsyncCommands { commands ->
            commands.del(playerKeySettingDataTag(player))
        }
    }
}
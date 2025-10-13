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

class RedisManager: ISyncCacheManager {

    companion object {
        const val SECOND_6_HOURS = 6 * 60 * 60L
        const val SECOND_12_HOURS = 12 * 60 * 60L
    }

    private val api by lazy { RedisChannelPlugin.commandAPI() }

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO> {
        debug("Redis 获取玩家 Profile")
        val tag = playerDataTag(player)
        val future = CompletableFuture<PlayerProfilePO>()
        try {
            api.useAsyncCommands { commands ->
                commands[tag].thenAccept { json ->
                    if (json == null) {
                        IStorageManager.INSTANCE.getPlayerData(player).thenAccept {
                            savePlayerProfile(player, it)
                            future.complete(it)
                        }
                    } else {
                        commands.expire(tag, SECOND_12_HOURS)
                        future.complete(Json.decodeFromString<PlayerProfilePO>(json))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            future.completeExceptionally(e)
        }
        return future
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        debug("Redis 获取玩家 Job")
        val tag = playerJobDataTag(player, id, job)
        val future = CompletableFuture<PlayerJobPO?>()
        try {
            api.useAsyncCommands { commands ->
                commands[tag].thenAccept { json ->
                    if (json == null) {
                        IStorageManager.INSTANCE.getPlayerJob(player, id, job).thenAccept {
                            it?.let { savePlayerJob(player, it) }
                            future.complete(it)
                        }
                    } else {
                        commands.expire(tag, SECOND_12_HOURS)
                        future.complete(Json.decodeFromString<PlayerJobPO>(json))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            future.completeExceptionally(e)
        }
        return future
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug("Redis 获取玩家 Skill")
        val tag = playerJobSkillDataTag(player, id, job, skill)
        val future = CompletableFuture<PlayerSkillPO?>()
        try {
            api.useAsyncCommands { commands ->
                commands[tag].thenAccept { json ->
                    if (json == null) {
                        IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill).thenAccept {
                            it?.let { savePlayerSkill(player, it) }
                            future.complete(it)
                        }
                    } else {
                        commands.expire(tag, SECOND_6_HOURS)
                        future.complete(Json.decodeFromString<PlayerSkillPO>(json))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            future.completeExceptionally(e)
        }
        return future
    }

    override fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?> {
        debug("Redis 获取玩家 KeySetting")
        val tag = playerKeySettingDataTag(player)
        val future = CompletableFuture<PlayerKeySettingPO?>()
        try {
            api.useAsyncCommands { commands ->
                commands[tag].thenAccept { json ->
                    if (json == null) {
                        IStorageManager.INSTANCE.getPlayerKey(id).thenAccept {
                            it?.let { savePlayerKeySetting(player, it) }
                            future.complete(it)
                        }
                    } else {
                        commands.expire(tag, SECOND_6_HOURS)
                        future.complete(Json.decodeFromString<PlayerKeySettingPO>(json))
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            future.completeExceptionally(e)
        }
        return future
    }

    override fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO) {
        debug("Redis 获取玩家 Profile")
        api.useAsyncCommands { commands ->
            commands.setex(playerDataTag(player), SECOND_12_HOURS, Json.encodeToString(playerProfilePO))
        }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO) {
        debug("Redis 获取玩家 Job")
        api.useAsyncCommands { commands ->
            commands.setex(playerJobDataTag(player, playerJobPO.id, playerJobPO.job), SECOND_12_HOURS, Json.encodeToString(playerJobPO))
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO) {
        debug("Redis 保存玩家 Skill")
        api.useAsyncCommands { commands ->
            commands.setex(playerJobSkillDataTag(player, playerSkillPO.id, playerSkillPO.job, playerSkillPO.skill), SECOND_6_HOURS, Json.encodeToString(playerSkillPO))
        }
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO) {
        debug("Redis 保存玩家 KeySetting")
        api.useAsyncCommands { commands ->
            commands.setex(playerKeySettingDataTag(player), SECOND_6_HOURS, Json.encodeToString(playerKeySettingPO))
        }
    }

    override fun removePlayerProfile(player: UUID) {
        debug("Redis 移除玩家 Profile")
        api.useAsyncCommands { commands ->
            commands.del(playerDataTag(player))
        }
    }

    override fun removePlayerJob(player: UUID, id: Int, job: String) {
        debug("Redis 移除玩家 Job")
        api.useAsyncCommands { commands ->
            commands.del(playerJobDataTag(player, id, job))
        }
    }

    override fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String) {
        debug("Redis 移除玩家 Skill")
        api.useAsyncCommands { commands ->
            commands.del(playerJobSkillDataTag(player, id, job, skill))
        }
    }

    override fun removePlayerKeySetting(player: UUID) {
        debug("Redis 移除玩家 KeySetting")
        api.useAsyncCommands { commands ->
            commands.del(playerKeySettingDataTag(player))
        }
    }
}
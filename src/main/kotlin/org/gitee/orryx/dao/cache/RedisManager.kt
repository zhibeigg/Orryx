package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.*
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Suppress("DuplicatedCode")
class RedisManager : ISyncCacheManager {

    companion object {
        const val SECOND_6_HOURS = 6 * 60 * 60L
        const val SECOND_12_HOURS = 12 * 60 * 60L
    }

    private val api by lazy { RedisChannelPlugin.commandAPI() }

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO> {
        debug { "Redis 获取玩家 Profile" }
        return readThrough(
            tag = playerDataTag(player),
            ttl = SECOND_12_HOURS,
            decode = { Json.decodeFromString<PlayerProfilePO>(it) },
            fallback = { IStorageManager.INSTANCE.getPlayerData(player) },
            warmCache = { savePlayerProfile(player, it) },
        )
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        debug { "Redis 获取玩家 Job" }
        return readThrough(
            tag = playerJobDataTag(player, id, job),
            ttl = SECOND_12_HOURS,
            decode = { Json.decodeFromString<PlayerJobPO>(it) },
            fallback = { IStorageManager.INSTANCE.getPlayerJob(player, id, job) },
            warmCache = { value -> value?.let { savePlayerJob(player, it) } },
        )
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        debug { "Redis 获取玩家 Skill" }
        return readThrough(
            tag = playerJobSkillDataTag(player, id, job, skill),
            ttl = SECOND_6_HOURS,
            decode = { Json.decodeFromString<PlayerSkillPO>(it) },
            fallback = { IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill) },
            warmCache = { value -> value?.let { savePlayerSkill(player, it) } },
        )
    }

    override fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?> {
        debug { "Redis 获取玩家 KeySetting" }
        return readThrough(
            tag = playerKeySettingDataTag(player),
            ttl = SECOND_6_HOURS,
            decode = { Json.decodeFromString<PlayerKeySettingPO>(it) },
            fallback = { IStorageManager.INSTANCE.getPlayerKey(id) },
            warmCache = { value -> value?.let { savePlayerKeySetting(player, it) } },
        )
    }

    override fun savePlayerProfileAsync(player: UUID, playerProfilePO: PlayerProfilePO): CompletableFuture<Unit> {
        return redisCommand { commands ->
            commands.setex(playerDataTag(player), SECOND_12_HOURS, Json.encodeToString(playerProfilePO))
        }
    }

    override fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO) {
        savePlayerProfileAsync(player, playerProfilePO).exceptionally { it.printStackTrace(); null }
    }

    override fun savePlayerJobAsync(player: UUID, playerJobPO: PlayerJobPO): CompletableFuture<Unit> {
        return redisCommand { commands ->
            commands.setex(playerJobDataTag(player, playerJobPO.id, playerJobPO.job), SECOND_12_HOURS, Json.encodeToString(playerJobPO))
        }
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO) {
        savePlayerJobAsync(player, playerJobPO).exceptionally { it.printStackTrace(); null }
    }

    override fun savePlayerSkillAsync(player: UUID, playerSkillPO: PlayerSkillPO): CompletableFuture<Unit> {
        return redisCommand { commands ->
            commands.setex(playerJobSkillDataTag(player, playerSkillPO.id, playerSkillPO.job, playerSkillPO.skill), SECOND_6_HOURS, Json.encodeToString(playerSkillPO))
        }
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO) {
        savePlayerSkillAsync(player, playerSkillPO).exceptionally { it.printStackTrace(); null }
    }

    override fun savePlayerKeySettingAsync(player: UUID, playerKeySettingPO: PlayerKeySettingPO): CompletableFuture<Unit> {
        return redisCommand { commands ->
            commands.setex(playerKeySettingDataTag(player), SECOND_6_HOURS, Json.encodeToString(playerKeySettingPO))
        }
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO) {
        savePlayerKeySettingAsync(player, playerKeySettingPO).exceptionally { it.printStackTrace(); null }
    }

    override fun removePlayerProfileAsync(player: UUID): CompletableFuture<Unit> {
        return redisCommand { commands -> commands.del(playerDataTag(player)) }
    }

    override fun removePlayerProfile(player: UUID) {
        removePlayerProfileAsync(player).exceptionally { it.printStackTrace(); null }
    }

    override fun removePlayerJobAsync(player: UUID, id: Int, job: String): CompletableFuture<Unit> {
        return redisCommand { commands -> commands.del(playerJobDataTag(player, id, job)) }
    }

    override fun removePlayerJob(player: UUID, id: Int, job: String) {
        removePlayerJobAsync(player, id, job).exceptionally { it.printStackTrace(); null }
    }

    override fun removePlayerSkillAsync(player: UUID, id: Int, job: String, skill: String): CompletableFuture<Unit> {
        return redisCommand { commands -> commands.del(playerJobSkillDataTag(player, id, job, skill)) }
    }

    override fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String) {
        removePlayerSkillAsync(player, id, job, skill).exceptionally { it.printStackTrace(); null }
    }

    override fun removePlayerKeySettingAsync(player: UUID): CompletableFuture<Unit> {
        return redisCommand { commands -> commands.del(playerKeySettingDataTag(player)) }
    }

    override fun removePlayerKeySetting(player: UUID) {
        removePlayerKeySettingAsync(player).exceptionally { it.printStackTrace(); null }
    }

    private fun <T> readThrough(
        tag: String,
        ttl: Long,
        decode: (String) -> T,
        fallback: () -> java.util.concurrent.CompletionStage<T>,
        warmCache: (T) -> Unit,
    ): CompletableFuture<T> {
        return redisReadFuture(
            executeCommands = { operation -> api.executeAsync { commands -> operation(commands) } },
            request = { commands -> commands[tag] },
            refreshExpiry = { commands -> commands.expire(tag, ttl) },
            decode = decode,
            fallback = fallback,
            warmCache = warmCache,
        )
    }

    private fun redisCommand(
        command: (RedisAsyncCommands<String, String>) -> java.util.concurrent.CompletionStage<*>,
    ): CompletableFuture<Unit> {
        return redisCommandFuture(
            executeCommands = { operation -> api.executeAsync { commands -> operation(commands) } },
            command = command,
        )
    }
}

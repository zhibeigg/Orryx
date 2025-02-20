package org.gitee.orryx.dao.cache

import com.gitee.redischannel.RedisChannelPlugin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gitee.orryx.dao.pojo.PlayerData
import org.gitee.orryx.dao.pojo.PlayerJob
import org.gitee.orryx.dao.pojo.PlayerSkill
import org.gitee.orryx.dao.storage.IStorageManager
import org.gitee.orryx.utils.playerDataTag
import org.gitee.orryx.utils.playerJobDataTag
import org.gitee.orryx.utils.playerJobSkillDataTag
import java.util.*

class RedisManager: ICacheManager {

    private val api by lazy { RedisChannelPlugin.api }

    override fun getPlayerData(player: UUID): PlayerData? {
        val tag = playerDataTag(player)
        var playerData = api.get(playerDataTag(player))?.let { Json.decodeFromString<PlayerData>(it) }
        if (playerData == null) {
            playerData = IStorageManager.INSTANCE.getPlayerData(player)
            playerData?.let { savePlayerData(player, it, true) }
        } else {
            api.refreshExpire(tag, 900, true)
        }
        return playerData
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJob? {
        val tag = playerJobDataTag(player, job)
        var jobData = api.get(playerJobDataTag(player, job))?.let { Json.decodeFromString<PlayerJob>(it) }
        if (jobData == null) {
            jobData = IStorageManager.INSTANCE.getPlayerJob(player, job)
            jobData?.let { savePlayerJob(player, it, true) }
        } else {
            api.refreshExpire(tag, 900, true)
        }
        return jobData
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkill? {
        val tag = playerJobSkillDataTag(player, job, skill)
        var skillData = api.get(tag)?.let { Json.decodeFromString<PlayerSkill>(it) }
        if (skillData == null) {
            skillData = IStorageManager.INSTANCE.getPlayerSkill(player, job, skill)
            skillData?.let { savePlayerSkill(player, it, true) }
        } else {
            api.refreshExpire(tag, 600, true)
        }
        return skillData
    }

    override fun savePlayerData(player: UUID, playerData: PlayerData, async: Boolean) {
        api.set(playerDataTag(player), Json.encodeToString(playerData), 900, async)
    }

    override fun savePlayerJob(player: UUID, playerJob: PlayerJob, async: Boolean) {
        api.set(playerJobDataTag(player, playerJob.job), Json.encodeToString(playerJob), 900, async)
    }

    override fun savePlayerSkill(player: UUID, playerSkill: PlayerSkill, async: Boolean) {
        api.set(playerJobSkillDataTag(player, playerSkill.job, playerSkill.skill), Json.encodeToString(playerSkill), 600, async)
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
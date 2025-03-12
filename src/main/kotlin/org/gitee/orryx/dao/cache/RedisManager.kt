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
import java.util.*

class RedisManager: ICacheManager {

    private val api by lazy { RedisChannelPlugin.api }

    override fun getPlayerData(player: UUID): PlayerProfilePO? {
        val tag = playerDataTag(player)
        var playerProfilePO = api.get(playerDataTag(player))?.let { Json.decodeFromString<PlayerProfilePO>(it) }
        if (playerProfilePO == null) {
            playerProfilePO = IStorageManager.INSTANCE.getPlayerData(player)
            playerProfilePO?.let { savePlayerData(player, it, true) }
        } else {
            api.refreshExpire(tag, 900, true)
        }
        return playerProfilePO
    }

    override fun getPlayerJob(player: UUID, job: String): PlayerJobPO? {
        val tag = playerJobDataTag(player, job)
        var jobData = api.get(playerJobDataTag(player, job))?.let { Json.decodeFromString<PlayerJobPO>(it) }
        if (jobData == null) {
            jobData = IStorageManager.INSTANCE.getPlayerJob(player, job)
            jobData?.let { savePlayerJob(player, it, true) }
        } else {
            api.refreshExpire(tag, 900, true)
        }
        return jobData
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): PlayerSkillPO? {
        val tag = playerJobSkillDataTag(player, job, skill)
        var skillData = api.get(tag)?.let { Json.decodeFromString<PlayerSkillPO>(it) }
        if (skillData == null) {
            skillData = IStorageManager.INSTANCE.getPlayerSkill(player, job, skill)
            skillData?.let { savePlayerSkill(player, it, true) }
        } else {
            api.refreshExpire(tag, 600, true)
        }
        return skillData
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
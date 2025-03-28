package org.gitee.orryx.dao.cache

import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import java.util.*
import java.util.concurrent.CompletableFuture

class DisableManager: ISyncCacheManager {

    override fun getPlayerData(player: UUID): CompletableFuture<PlayerProfilePO?> {
        return IStorageManager.INSTANCE.getPlayerData(player)
    }

    override fun getPlayerJob(player: UUID, job: String): CompletableFuture<PlayerJobPO?> {
        return IStorageManager.INSTANCE.getPlayerJob(player, job)
    }

    override fun getPlayerSkill(player: UUID, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        return IStorageManager.INSTANCE.getPlayerSkill(player, job, skill)
    }

    override fun savePlayerData(player: UUID, playerProfilePO: PlayerProfilePO, async: Boolean) {
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
    }

    override fun removePlayerData(player: UUID, async: Boolean) {
    }

    override fun removePlayerJob(player: UUID, job: String, async: Boolean) {
    }

    override fun removePlayerSkill(player: UUID, job: String, skill: String, async: Boolean) {
    }

}
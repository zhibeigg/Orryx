package org.gitee.orryx.dao.cache

import org.gitee.orryx.dao.pojo.PlayerJobPO
import org.gitee.orryx.dao.pojo.PlayerKeySettingPO
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import org.gitee.orryx.dao.pojo.PlayerSkillPO
import org.gitee.orryx.dao.storage.IStorageManager
import java.util.*
import java.util.concurrent.CompletableFuture

class DisableManager: ISyncCacheManager {

    override fun getPlayerProfile(player: UUID): CompletableFuture<PlayerProfilePO> {
        return IStorageManager.INSTANCE.getPlayerData(player)
    }

    override fun getPlayerJob(player: UUID, id: Int, job: String): CompletableFuture<PlayerJobPO?> {
        return IStorageManager.INSTANCE.getPlayerJob(player, id, job)
    }

    override fun getPlayerSkill(player: UUID, id: Int, job: String, skill: String): CompletableFuture<PlayerSkillPO?> {
        return IStorageManager.INSTANCE.getPlayerSkill(player, id, job, skill)
    }

    override fun getPlayerKeySetting(player: UUID, id: Int): CompletableFuture<PlayerKeySettingPO?> {
        return IStorageManager.INSTANCE.getPlayerKey(id)
    }

    override fun savePlayerProfile(player: UUID, playerProfilePO: PlayerProfilePO, async: Boolean) {
    }

    override fun savePlayerJob(player: UUID, playerJobPO: PlayerJobPO, async: Boolean) {
    }

    override fun savePlayerSkill(player: UUID, playerSkillPO: PlayerSkillPO, async: Boolean) {
    }

    override fun savePlayerKeySetting(player: UUID, playerKeySettingPO: PlayerKeySettingPO, async: Boolean) {
    }

    override fun removePlayerProfile(player: UUID, async: Boolean) {
    }

    override fun removePlayerJob(player: UUID, id: Int, job: String, async: Boolean) {
    }

    override fun removePlayerSkill(player: UUID, id: Int, job: String, skill: String, async: Boolean) {
    }

    override fun removePlayerKeySetting(player: UUID, async: Boolean) {
    }
}
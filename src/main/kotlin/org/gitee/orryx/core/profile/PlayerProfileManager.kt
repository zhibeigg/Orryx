package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.job.IPlayerJob
import org.gitee.orryx.core.job.PlayerJob
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.utils.DEFAULT
import org.gitee.orryx.utils.playerData
import taboolib.common.platform.event.SubscribeEvent
import java.util.*

object PlayerProfileManager {

    private val playerProfileMap by lazy { mutableMapOf<UUID, IPlayerProfile>() }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerProfileMap.remove(e.player.uniqueId)
    }

    fun Player.orryxProfile(): IPlayerProfile {
        playerProfileMap[uniqueId]?.let { return it }
        playerData()?.let { PlayerProfile(this, it.job, it.point, it.flags.toMutableMap()) }?.let {
            playerProfileMap[uniqueId] = it
            return it
        }
        PlayerProfile(this, null, 0, mutableMapOf()).let {
            playerProfileMap[uniqueId] = it
            return it
        }
    }

    fun <T> Player.job(function: (IPlayerJob) -> T): T? {
        return job()?.let {
            function(it)
        }
    }

    fun Player.job(): IPlayerJob? {
        val job = orryxProfile().job ?: return null
        return job(job)
    }

    fun Player.job(job: String): IPlayerJob {
        return ICacheManager.INSTANCE.getPlayerJob(uniqueId, job)?.let { PlayerJob(this, it.job, it.experience, it.group, it.bindKeyOfGroup.mapValues { map -> map.value.toMutableMap() }.toMutableMap()) } ?: defaultJob(job).apply { save(true) }
    }

    private fun Player.defaultJob(job: String) = PlayerJob(this, job, 0, DEFAULT, mutableMapOf())

}
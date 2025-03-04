package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.utils.playerData
import org.gitee.orryx.utils.toFlag
import taboolib.common.platform.event.SubscribeEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerProfileManager {

    private val playerProfileMap by lazy { HashMap<UUID, IPlayerProfile>() }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerProfileMap.remove(e.player.uniqueId)
    }

    fun Player.orryxProfile(): IPlayerProfile {
        playerProfileMap[uniqueId]?.let { return it }
        return playerData()?.let {
            val list = it.flags.mapNotNull { (key, value) ->
                value.toFlag()?.let { flag -> key to flag }
            }
            PlayerProfile(this, it.job, it.point, list.toMap(ConcurrentHashMap(list.size)))
        }?.also {
            playerProfileMap[uniqueId] = it
        } ?: PlayerProfile(this, null, 0, ConcurrentHashMap()).also {
            playerProfileMap[uniqueId] = it
        }
    }

}
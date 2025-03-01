package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.utils.playerData
import org.gitee.orryx.utils.toFlag
import taboolib.common.platform.event.SubscribeEvent
import java.util.*

object PlayerProfileManager {

    private val playerProfileMap by lazy { HashMap<UUID, IPlayerProfile>() }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerProfileMap.remove(e.player.uniqueId)
    }

    fun Player.orryxProfile(): IPlayerProfile {
        playerProfileMap[uniqueId]?.let { return it }
        playerData()?.let { PlayerProfile(this, it.job, it.point, it.flags.mapNotNull { (key, value) ->
            value.toFlag()?.let { key to it }
        }.toMap(HashMap())) }?.let {
            playerProfileMap[uniqueId] = it
            return it
        }
        PlayerProfile(this, null, 0, HashMap()).let {
            playerProfileMap[uniqueId] = it
            return it
        }
    }

}
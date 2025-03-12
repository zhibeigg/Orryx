package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerProfilePO
import java.util.*

internal fun Player.playerData(): PlayerProfilePO? = uniqueId.playerData()

internal fun UUID.playerData(): PlayerProfilePO? {
    return ICacheManager.INSTANCE.getPlayerData(this)
}
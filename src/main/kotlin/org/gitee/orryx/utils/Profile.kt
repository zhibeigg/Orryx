package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.dao.cache.ICacheManager
import org.gitee.orryx.dao.pojo.PlayerData
import java.util.*

internal fun Player.playerData(): PlayerData? = uniqueId.playerData()

internal fun UUID.playerData(): PlayerData? {
    return ICacheManager.INSTANCE.getPlayerData(this)
}
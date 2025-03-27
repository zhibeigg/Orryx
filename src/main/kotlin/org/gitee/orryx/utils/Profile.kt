package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache

fun Player.orryxProfile(): IPlayerProfile {
    return MemoryCache.getPlayerProfile(this)
}
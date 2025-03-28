package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache
import java.util.concurrent.CompletableFuture

fun Player.orryxProfile(): CompletableFuture<IPlayerProfile> {
    return MemoryCache.getPlayerProfile(this)
}

fun <T> Player.orryxProfile(func: (profile: IPlayerProfile) -> T): CompletableFuture<T> {
    return orryxProfile().thenApply {
        func(it)
    }
}
package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun Player.orryxProfile(): CompletableFuture<IPlayerProfile> {
    return MemoryCache.getPlayerProfile(this)
}

fun <T> Player.orryxProfileTo(func: (profile: IPlayerProfile) -> T): CompletableFuture<T> {
    return orryxProfile().thenApply {
        func(it)
    }
}

fun <T> Player.orryxProfile(func: (profile: IPlayerProfile) -> CompletionStage<T>?): CompletableFuture<T> {
    return orryxProfile().thenCompose {
        func(it)
    }
}
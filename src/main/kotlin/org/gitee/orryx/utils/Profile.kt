package org.gitee.orryx.utils

import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IFlag
import org.gitee.orryx.core.profile.IPlayerProfile
import org.gitee.orryx.core.profile.PlayerProfile
import org.gitee.orryx.dao.cache.MemoryCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

fun Player.orryxProfile(): CompletableFuture<IPlayerProfile> {
    return MemoryCache.getPlayerProfile(uniqueId)
}

inline fun <T> Player.orryxProfileTo(crossinline func: (profile: IPlayerProfile) -> T): CompletableFuture<T> {
    return orryxProfile().thenApplyMain {
        func(it)
    }
}

inline fun <T> Player.orryxProfile(crossinline func: (profile: IPlayerProfile) -> CompletionStage<T>?): CompletableFuture<T> {
    return orryxProfile().thenComposeMain { profile ->
        func(profile) ?: CompletableFuture<T>().also {
            it.completeExceptionally(IllegalStateException("Profile callback returned null CompletionStage"))
        }
    }
}

fun IPlayerProfile.setFlagFuture(flagName: String, flag: IFlag, save: Boolean = true): CompletableFuture<Boolean> {
    return if (this is PlayerProfile) {
        setFlagAsync(flagName, flag, save)
    } else {
        mainThreadFuture {
            setFlag(flagName, flag, save)
            true
        }
    }
}

fun IPlayerProfile.removeFlagFuture(flagName: String, save: Boolean = true): CompletableFuture<IFlag?> {
    return if (this is PlayerProfile) {
        removeFlagAsync(flagName, save)
    } else {
        mainThreadFuture { removeFlag(flagName, save) }
    }
}

fun IPlayerProfile.clearFlagsFuture(): CompletableFuture<Unit> {
    return if (this is PlayerProfile) {
        clearFlagsAsync()
    } else {
        mainThreadFuture { clearFlags() }
    }
}
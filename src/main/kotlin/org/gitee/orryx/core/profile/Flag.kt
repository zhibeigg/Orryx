package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.gitee.orryx.utils.orryxProfileTo
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

open class Flag<T: Any>(override val value: T, override val isPersistence: Boolean, override val timeout: Long) : IFlag {

    override val timestamp: Long = System.currentTimeMillis()

    val taskMap = ConcurrentHashMap<UUID, ConcurrentHashMap<String, PlatformExecutor.PlatformTask>>()

    override fun init(player: Player, key: String) {
        if (timeout == 0L) return
        taskMap.getOrPut(player.uniqueId) { ConcurrentHashMap() }[key] = submit(delay = timeout / 50) {
            if (player.isOnline) {
                player.orryxProfileTo { profile ->
                    profile.removeFlag(key)
                }
            }
        }
    }

    override fun cancel(player: Player, key: String) {
        taskMap[player.uniqueId]?.get(key)?.cancel()
    }

    override fun toString(): String {
        return "Flag(value=$value, isPersistence=$isPersistence, timeout=$timeout, timestamp=$timestamp)"
    }
}
package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.utils.orryxProfileTo
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class Flag<T: Any>(
    override val value: T,
    override val isPersistence: Boolean,
    override val timeout: Long,
    expiresAt: Long = 0L
) : IFlag {

    override val timestamp: Long = System.currentTimeMillis()
    override val expiresAt: Long = when {
        timeout <= 0L -> 0L
        expiresAt > 0L -> expiresAt
        timestamp > Long.MAX_VALUE - timeout -> Long.MAX_VALUE
        else -> timestamp + timeout
    }

    override fun init(player: Player, key: String) {
        if (expiresAt == 0L) return
        val remaining = positiveDifference(expiresAt, System.currentTimeMillis())
        val tasks = taskMap.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }
        lateinit var scheduled: PlatformExecutor.PlatformTask
        scheduled = submit(delay = remaining / 50L + 1L) {
            if (!tasks.remove(key, scheduled)) return@submit
            if (tasks.isEmpty()) taskMap.remove(player.uniqueId, tasks)
            if (player.isOnline && isTimeout()) {
                player.orryxProfileTo { profile ->
                    if (profile is PlayerProfile) {
                        profile.removeFlagIfSameAsync(key, this@Flag).exceptionally { it.printStackTrace(); null }
                    } else if (profile.getFlag(key) === this@Flag) {
                        profile.removeFlag(key)
                    }
                }
            }
        }
        tasks.put(key, scheduled)?.cancel()
    }

    override fun cancel(player: Player, key: String) {
        taskMap[player.uniqueId]?.remove(key)?.cancel()
    }

    override fun toString(): String {
        return "Flag(value=$value, isPersistence=$isPersistence, timeout=$timeout, timestamp=$timestamp)"
    }

    companion object {
        /**
         * 全局任务Map，用于管理所有Flag的定时任务
         * 使用伴生对象确保所有Flag实例共享同一个Map，便于统一清理
         */
        private val taskMap = ConcurrentHashMap<UUID, ConcurrentHashMap<String, PlatformExecutor.PlatformTask>>()

        private fun positiveDifference(end: Long, start: Long): Long {
            if (end <= start) return 0L
            return if (start < 0L && end > Long.MAX_VALUE + start) Long.MAX_VALUE else end - start
        }

        /**
         * 清理指定玩家的所有Flag任务
         */
        internal fun cleanupPlayer(playerUuid: UUID) {
            taskMap.remove(playerUuid)?.forEach { (_, task) ->
                try {
                    task.cancel()
                } catch (_: Exception) {
                    // 忽略取消时的异常
                }
            }
        }

        /**
         * 玩家退出时清理任务，防止内存泄漏
         */
        @SubscribeEvent
        private fun onPlayerQuit(e: PlayerQuitEvent) {
            cleanupPlayer(e.player.uniqueId)
        }
    }
}
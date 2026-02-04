package org.gitee.orryx.core.profile

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.utils.orryxProfileTo
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

open class Flag<T: Any>(override val value: T, override val isPersistence: Boolean, override val timeout: Long) : IFlag {

    override val timestamp: Long = System.currentTimeMillis()

    override fun init(player: Player, key: String) {
        if (timeout == 0L) return
        taskMap.getOrPut(player.uniqueId) { ConcurrentHashMap() }[key] = submit(delay = timeout / 50) {
            // 任务完成后自动从 taskMap 中移除
            taskMap[player.uniqueId]?.remove(key)
            if (player.isOnline) {
                player.orryxProfileTo { profile ->
                    profile.removeFlag(key)
                }
            }
        }
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
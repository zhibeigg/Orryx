package org.gitee.orryx.core.profile

import org.bukkit.entity.Player

sealed interface IFlag {

    /**
     * 值
     * */
    val value: Any

    /**
     * 是否持久化
     * */
    val isPersistence: Boolean

    /**
     * 出生时间戳 (毫秒)
     * */
    val timestamp: Long

    /**
     * 存活时间 (毫秒)
     *
     * 0 为永久
     * */
    val timeout: Long

    /**
     * 是否死亡
     * */
    fun isTimeout(): Boolean {
        return  timeout != 0L && timeout + timestamp < System.currentTimeMillis()
    }

    /**
     * 出生
     * */
    fun init(player: Player, key: String)

    /**
     * 取消
     * */
    fun cancel(player: Player, key: String)
}
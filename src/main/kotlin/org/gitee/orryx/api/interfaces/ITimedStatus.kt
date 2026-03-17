package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player

/**
 * 限时状态接口。
 *
 * 统一管理霸体、无敌、免疫摔伤、沉默等限时状态。
 */
interface ITimedStatus {

    /**
     * 是否处于该状态。
     *
     * @param player 玩家
     * @return 是否处于该状态
     */
    fun isActive(player: Player): Boolean

    /**
     * 剩余时间（毫秒），未激活返回 0。
     *
     * @param player 玩家
     * @return 剩余时间（毫秒）
     */
    fun countdown(player: Player): Long

    /**
     * 设置状态持续时间（毫秒），仅当新时间大于剩余时间时生效。
     *
     * @param player 玩家
     * @param timeout 持续时间（毫秒）
     */
    fun set(player: Player, timeout: Long)

    /**
     * 取消状态。
     *
     * @param player 玩家
     */
    fun cancel(player: Player)

    /**
     * 延长状态时间（毫秒）。
     *
     * @param player 玩家
     * @param timeout 延长时间（毫秒）
     */
    fun add(player: Player, timeout: Long)

    /**
     * 缩短状态时间（毫秒）。
     *
     * @param player 玩家
     * @param timeout 缩短时间（毫秒）
     */
    fun reduce(player: Player, timeout: Long)
}

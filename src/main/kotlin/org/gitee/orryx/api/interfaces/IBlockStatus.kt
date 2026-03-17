package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import java.util.function.Consumer

/**
 * 格挡状态接口。
 *
 * 格挡状态按伤害类型分别管理，每种伤害类型可独立设置。
 */
interface IBlockStatus {

    /**
     * 是否处于指定类型的格挡状态。
     *
     * @param player 玩家
     * @param type 伤害类型
     * @return 是否处于格挡状态
     */
    fun isActive(player: Player, type: DamageType): Boolean

    /**
     * 获取指定类型格挡的剩余时间（毫秒），未激活返回 0。
     *
     * @param player 玩家
     * @param type 伤害类型
     * @return 剩余时间（毫秒）
     */
    fun countdown(player: Player, type: DamageType): Long

    /**
     * 设置指定类型的格挡时间。
     *
     * @param player 玩家
     * @param type 伤害类型
     * @param timeout 持续时间（毫秒）
     * @param onSuccess 格挡成功时执行的回调
     */
    fun set(player: Player, type: DamageType, timeout: Long, onSuccess: Consumer<OrryxDamageEvents.Pre>)

    /**
     * 取消指定类型的格挡。
     *
     * @param player 玩家
     * @param type 伤害类型
     */
    fun cancel(player: Player, type: DamageType)

    /**
     * 取消所有格挡。
     *
     * @param player 玩家
     */
    fun cancelAll(player: Player)

    /**
     * 延长指定类型的格挡时间。
     *
     * @param player 玩家
     * @param type 伤害类型
     * @param timeout 延长时间（毫秒）
     */
    fun add(player: Player, type: DamageType, timeout: Long)

    /**
     * 缩短指定类型的格挡时间。
     *
     * @param player 玩家
     * @param type 伤害类型
     * @param timeout 缩短时间（毫秒）
     */
    fun reduce(player: Player, type: DamageType, timeout: Long)
}

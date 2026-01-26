package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player
import org.gitee.orryx.api.events.damage.DamageType
import org.gitee.orryx.api.events.damage.OrryxDamageEvents
import org.gitee.orryx.core.profile.IPlayerProfile
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function

/**
 * 玩家档案 API 接口
 *
 * 提供对玩家状态（霸体、无敌、免疫摔伤、格挡、沉默等）的管理功能
 *
 * 注意：此 API 中的状态管理方法（如霸体、无敌等）仅在内存中生效，不会持久化保存
 * */
interface IProfileAPI {

    /**
     * 获取玩家信息并进行操作
     *
     * 注意：此方法不会自动保存更改，如需持久化请在修改后手动调用 [IPlayerProfile.save] 方法
     *
     * @param player 玩家
     * @param function 获取到玩家信息后执行的函数
     * @return [function] 的返回值，如果玩家信息不存在则返回 null
     * */
    fun <T> modifyProfile(player: Player, function: Function<IPlayerProfile, T>) : CompletableFuture<T?>

    /**
     * 是否在霸体状态
     *
     * @param player 玩家
     * @return 是否在霸体状态
     */
    fun isSuperBody(player: Player): Boolean

    /**
     * 获取霸体状态的倒计时
     *
     * @param player 玩家
     * @return 剩余时间（毫秒）
     */
    fun superBodyCountdown(player: Player): Long

    /**
     * 设置霸体时间
     *
     * @param player 玩家
     * @param timeout 霸体时长（毫秒）
     */
    fun setSuperBody(player: Player, timeout: Long)

    /**
     * 取消霸体
     *
     * @param player 玩家
     */
    fun cancelSuperBody(player: Player)

    /**
     * 延长霸体时间
     *
     * @param player 玩家
     * @param timeout 延长的霸体时长（毫秒）
     */
    fun addSuperBody(player: Player, timeout: Long)

    /**
     * 减少霸体时间
     *
     * @param player 玩家
     * @param timeout 减少的霸体时长（毫秒）
     */
    fun reduceSuperBody(player: Player, timeout: Long)

    /**
     * 是否在无敌状态
     *
     * @param player 玩家
     * @return 是否在无敌状态
     */
    fun isInvincible(player: Player): Boolean

    /**
     * 获取无敌状态的倒计时
     *
     * @param player 玩家
     * @return 剩余时间（毫秒）
     */
    fun invincibleCountdown(player: Player): Long

    /**
     * 设置无敌时间
     *
     * @param player 玩家
     * @param timeout 无敌时长（毫秒）
     */
    fun setInvincible(player: Player, timeout: Long)

    /**
     * 取消无敌
     *
     * @param player 玩家
     */
    fun cancelInvincible(player: Player)

    /**
     * 延长无敌时间
     *
     * @param player 玩家
     * @param timeout 延长的无敌时长（毫秒）
     */
    fun addInvincible(player: Player, timeout: Long)

    /**
     * 减少无敌时间
     *
     * @param player 玩家
     * @param timeout 减少的无敌时长（毫秒）
     */
    fun reduceInvincible(player: Player, timeout: Long)

    /**
     * 是否在免疫摔伤状态
     *
     * @param player 玩家
     * @return 是否在免疫摔伤状态
     */
    fun isSuperFoot(player: Player): Boolean

    /**
     * 获取免疫摔伤状态的倒计时
     *
     * @param player 玩家
     * @return 剩余时间（毫秒）
     */
    fun superFootCountdown(player: Player): Long

    /**
     * 设置免疫摔伤时间
     *
     * @param player 玩家
     * @param timeout 免疫摔伤时长（毫秒）
     */
    fun setSuperFoot(player: Player, timeout: Long)

    /**
     * 取消免疫摔伤
     *
     * @param player 玩家
     */
    fun cancelSuperFoot(player: Player)

    /**
     * 延长免疫摔伤时间
     *
     * @param player 玩家
     * @param timeout 延长的免疫摔伤时长（毫秒）
     */
    fun addSuperFoot(player: Player, timeout: Long)

    /**
     * 减少免疫摔伤时间
     *
     * @param player 玩家
     * @param timeout 减少的免疫摔伤时长（毫秒）
     */
    fun reduceSuperFoot(player: Player, timeout: Long)

    /**
     * 是否在格挡状态
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     * @return 是否在格挡状态
     */
    fun isBlock(player: Player, blockType: DamageType): Boolean

    /**
     * 获取格挡状态的倒计时
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     * @return 剩余时间（毫秒）
     */
    fun blockCountdown(player: Player, blockType: DamageType): Long

    /**
     * 设置格挡时间
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     * @param timeout 格挡时长（毫秒）
     * @param success 格挡成功时执行的回调
     */
    fun setBlock(player: Player, blockType: DamageType, timeout: Long, success: Consumer<OrryxDamageEvents.Pre>)

    /**
     * 取消指定类型的格挡
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     */
    fun cancelBlock(player: Player, blockType: DamageType)

    /**
     * 取消所有格挡
     *
     * @param player 玩家
     */
    fun cancelBlock(player: Player)

    /**
     * 延长格挡时间
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     * @param timeout 延长的格挡时长（毫秒）
     */
    fun addBlock(player: Player, blockType: DamageType, timeout: Long)

    /**
     * 减少格挡时间
     *
     * @param player 玩家
     * @param blockType 格挡的伤害类型
     * @param timeout 减少的格挡时长（毫秒）
     */
    fun reduceBlock(player: Player, blockType: DamageType, timeout: Long)

    /**
     * 是否在沉默状态
     *
     * @param player 玩家
     * @return 是否在沉默状态
     */
    fun isSilence(player: Player): Boolean

    /**
     * 获取沉默状态的倒计时
     *
     * @param player 玩家
     * @return 剩余时间（毫秒）
     */
    fun silenceCountdown(player: Player): Long

    /**
     * 设置沉默时间
     *
     * @param player 玩家
     * @param timeout 沉默时长（毫秒）
     */
    fun setSilence(player: Player, timeout: Long)

    /**
     * 取消沉默
     *
     * @param player 玩家
     */
    fun cancelSilence(player: Player)

    /**
     * 延长沉默时间
     *
     * @param player 玩家
     * @param timeout 延长的沉默时长（毫秒）
     */
    fun addSilence(player: Player, timeout: Long)

    /**
     * 减少沉默时间
     *
     * @param player 玩家
     * @param timeout 减少的沉默时长（毫秒）
     */
    fun reduceSilence(player: Player, timeout: Long)
}
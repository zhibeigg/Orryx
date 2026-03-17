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
     * 霸体状态管理
     */
    fun superBody(): ITimedStatus

    /**
     * 无敌状态管理
     */
    fun invincible(): ITimedStatus

    /**
     * 免疫摔伤状态管理
     */
    fun superFoot(): ITimedStatus

    /**
     * 沉默状态管理
     */
    fun silence(): ITimedStatus

    /**
     * 格挡状态管理
     */
    fun block(): IBlockStatus

    // ==================== 以下旧方法标记 @Deprecated，保留一个大版本 ====================

    /**
     * 是否在霸体状态
     */
    @Deprecated("使用 superBody().isActive(player)", ReplaceWith("superBody().isActive(player)"))
    fun isSuperBody(player: Player): Boolean = superBody().isActive(player)

    /**
     * 获取霸体状态的倒计时
     */
    @Deprecated("使用 superBody().countdown(player)", ReplaceWith("superBody().countdown(player)"))
    fun superBodyCountdown(player: Player): Long = superBody().countdown(player)

    /**
     * 设置霸体时间
     */
    @Deprecated("使用 superBody().set(player, timeout)", ReplaceWith("superBody().set(player, timeout)"))
    fun setSuperBody(player: Player, timeout: Long) = superBody().set(player, timeout)

    /**
     * 取消霸体
     */
    @Deprecated("使用 superBody().cancel(player)", ReplaceWith("superBody().cancel(player)"))
    fun cancelSuperBody(player: Player) = superBody().cancel(player)

    /**
     * 延长霸体时间
     */
    @Deprecated("使用 superBody().add(player, timeout)", ReplaceWith("superBody().add(player, timeout)"))
    fun addSuperBody(player: Player, timeout: Long) = superBody().add(player, timeout)

    /**
     * 减少霸体时间
     */
    @Deprecated("使用 superBody().reduce(player, timeout)", ReplaceWith("superBody().reduce(player, timeout)"))
    fun reduceSuperBody(player: Player, timeout: Long) = superBody().reduce(player, timeout)

    /**
     * 是否在无敌状态
     */
    @Deprecated("使用 invincible().isActive(player)", ReplaceWith("invincible().isActive(player)"))
    fun isInvincible(player: Player): Boolean = invincible().isActive(player)

    /**
     * 获取无敌状态的倒计时
     */
    @Deprecated("使用 invincible().countdown(player)", ReplaceWith("invincible().countdown(player)"))
    fun invincibleCountdown(player: Player): Long = invincible().countdown(player)

    /**
     * 设置无敌时间
     */
    @Deprecated("使用 invincible().set(player, timeout)", ReplaceWith("invincible().set(player, timeout)"))
    fun setInvincible(player: Player, timeout: Long) = invincible().set(player, timeout)

    /**
     * 取消无敌
     */
    @Deprecated("使用 invincible().cancel(player)", ReplaceWith("invincible().cancel(player)"))
    fun cancelInvincible(player: Player) = invincible().cancel(player)

    /**
     * 延长无敌时间
     */
    @Deprecated("使用 invincible().add(player, timeout)", ReplaceWith("invincible().add(player, timeout)"))
    fun addInvincible(player: Player, timeout: Long) = invincible().add(player, timeout)

    /**
     * 减少无敌时间
     */
    @Deprecated("使用 invincible().reduce(player, timeout)", ReplaceWith("invincible().reduce(player, timeout)"))
    fun reduceInvincible(player: Player, timeout: Long) = invincible().reduce(player, timeout)

    /**
     * 是否在免疫摔伤状态
     */
    @Deprecated("使用 superFoot().isActive(player)", ReplaceWith("superFoot().isActive(player)"))
    fun isSuperFoot(player: Player): Boolean = superFoot().isActive(player)

    /**
     * 获取免疫摔伤状态的倒计时
     */
    @Deprecated("使用 superFoot().countdown(player)", ReplaceWith("superFoot().countdown(player)"))
    fun superFootCountdown(player: Player): Long = superFoot().countdown(player)

    /**
     * 设置免疫摔伤时间
     */
    @Deprecated("使用 superFoot().set(player, timeout)", ReplaceWith("superFoot().set(player, timeout)"))
    fun setSuperFoot(player: Player, timeout: Long) = superFoot().set(player, timeout)

    /**
     * 取消免疫摔伤
     */
    @Deprecated("使用 superFoot().cancel(player)", ReplaceWith("superFoot().cancel(player)"))
    fun cancelSuperFoot(player: Player) = superFoot().cancel(player)

    /**
     * 延长免疫摔伤时间
     */
    @Deprecated("使用 superFoot().add(player, timeout)", ReplaceWith("superFoot().add(player, timeout)"))
    fun addSuperFoot(player: Player, timeout: Long) = superFoot().add(player, timeout)

    /**
     * 减少免疫摔伤时间
     */
    @Deprecated("使用 superFoot().reduce(player, timeout)", ReplaceWith("superFoot().reduce(player, timeout)"))
    fun reduceSuperFoot(player: Player, timeout: Long) = superFoot().reduce(player, timeout)

    /**
     * 是否在格挡状态
     */
    @Deprecated("使用 block().isActive(player, blockType)", ReplaceWith("block().isActive(player, blockType)"))
    fun isBlock(player: Player, blockType: DamageType): Boolean = block().isActive(player, blockType)

    /**
     * 获取格挡状态的倒计时
     */
    @Deprecated("使用 block().countdown(player, blockType)", ReplaceWith("block().countdown(player, blockType)"))
    fun blockCountdown(player: Player, blockType: DamageType): Long = block().countdown(player, blockType)

    /**
     * 设置格挡时间
     */
    @Deprecated("使用 block().set(player, blockType, timeout, success)", ReplaceWith("block().set(player, blockType, timeout, success)"))
    fun setBlock(player: Player, blockType: DamageType, timeout: Long, success: Consumer<OrryxDamageEvents.Pre>) = block().set(player, blockType, timeout, success)

    /**
     * 取消指定类型的格挡
     */
    @Deprecated("使用 block().cancel(player, blockType)", ReplaceWith("block().cancel(player, blockType)"))
    fun cancelBlock(player: Player, blockType: DamageType) = block().cancel(player, blockType)

    /**
     * 取消所有格挡
     */
    @Deprecated("使用 block().cancelAll(player)", ReplaceWith("block().cancelAll(player)"))
    fun cancelBlock(player: Player) = block().cancelAll(player)

    /**
     * 延长格挡时间
     */
    @Deprecated("使用 block().add(player, blockType, timeout)", ReplaceWith("block().add(player, blockType, timeout)"))
    fun addBlock(player: Player, blockType: DamageType, timeout: Long) = block().add(player, blockType, timeout)

    /**
     * 减少格挡时间
     */
    @Deprecated("使用 block().reduce(player, blockType, timeout)", ReplaceWith("block().reduce(player, blockType, timeout)"))
    fun reduceBlock(player: Player, blockType: DamageType, timeout: Long) = block().reduce(player, blockType, timeout)

    /**
     * 是否在沉默状态
     */
    @Deprecated("使用 silence().isActive(player)", ReplaceWith("silence().isActive(player)"))
    fun isSilence(player: Player): Boolean = silence().isActive(player)

    /**
     * 获取沉默状态的倒计时
     */
    @Deprecated("使用 silence().countdown(player)", ReplaceWith("silence().countdown(player)"))
    fun silenceCountdown(player: Player): Long = silence().countdown(player)

    /**
     * 设置沉默时间
     */
    @Deprecated("使用 silence().set(player, timeout)", ReplaceWith("silence().set(player, timeout)"))
    fun setSilence(player: Player, timeout: Long) = silence().set(player, timeout)

    /**
     * 取消沉默
     */
    @Deprecated("使用 silence().cancel(player)", ReplaceWith("silence().cancel(player)"))
    fun cancelSilence(player: Player) = silence().cancel(player)

    /**
     * 延长沉默时间
     */
    @Deprecated("使用 silence().add(player, timeout)", ReplaceWith("silence().add(player, timeout)"))
    fun addSilence(player: Player, timeout: Long) = silence().add(player, timeout)

    /**
     * 减少沉默时间
     */
    @Deprecated("使用 silence().reduce(player, timeout)", ReplaceWith("silence().reduce(player, timeout)"))
    fun reduceSilence(player: Player, timeout: Long) = silence().reduce(player, timeout)
}

package org.gitee.orryx.api.interfaces

import org.bukkit.entity.Player

interface IProfileAPI {

    /**
     * 是否在霸体状态
     * @return 是否在霸体状态
     */
    fun isSuperBody(player: Player): Boolean

    /**
     * 获取霸体状态的倒计时
     * @return [Long] 毫秒
     */
    fun superBodyCountdown(player: Player): Long

    /**
     * 设置霸体时间
     * @param timeout 霸体时长（毫秒）
     */
    fun setSuperBody(player: Player, timeout: Long)

    /**
     * 取消霸体
     */
    fun cancelSuperBody(player: Player)

    /**
     * 延长霸体时间
     * @param timeout 延长的霸体时长（毫秒）
     */
    fun addSuperBody(player: Player, timeout: Long)

    /**
     * 减少霸体时间
     * @param timeout 减少的霸体时长（毫秒）
     */
    fun reduceSuperBody(player: Player, timeout: Long)

    /**
     * 是否在无敌状态
     * @return 是否在无敌状态
     */
    fun isInvincible(player: Player): Boolean

    /**
     * 获取无敌状态的倒计时
     * @return [Long] 毫秒
     */
    fun invincibleCountdown(player: Player): Long

    /**
     * 设置无敌时间
     * @param timeout 霸体时长（毫秒）
     */
    fun setInvincible(player: Player, timeout: Long)

    /**
     * 取消无敌
     */
    fun cancelInvincible(player: Player)

    /**
     * 延长无敌时间
     * @param timeout 延长的霸体时长（毫秒）
     */
    fun addInvincible(player: Player, timeout: Long)

    /**
     * 减少无敌时间
     * @param timeout 减少的霸体时长（毫秒）
     */
    fun reduceInvincible(player: Player, timeout: Long)

    /**
     * 是否在免疫摔伤状态
     * @return 是否在免疫摔伤状态
     */
    fun isSuperFoot(player: Player): Boolean

    /**
     * 获取免疫摔伤状态的倒计时
     * @return [Long] 毫秒
     */
    fun superFootCountdown(player: Player): Long

    /**
     * 设置免疫摔伤时间
     * @param timeout 免疫摔伤时长（毫秒）
     */
    fun setSuperFoot(player: Player, timeout: Long)

    /**
     * 取消免疫摔伤
     */
    fun cancelSuperFoot(player: Player)

    /**
     * 延长免疫摔伤时间
     * @param timeout 延长的免疫摔伤时长（毫秒）
     */
    fun addSuperFoot(player: Player, timeout: Long)

    /**
     * 减少免疫摔伤时间
     * @param timeout 减少的免疫摔伤时长（毫秒）
     */
    fun reduceSuperFoot(player: Player, timeout: Long)

    /**
     * 是否在格挡状态
     * @return 是否在格挡状态
     */
    fun isBlock(player: Player): Boolean

    /**
     * 获取格挡状态的倒计时
     * @return [Long] 毫秒
     */
    fun blockCountdown(player: Player): Long

    /**
     * 设置格挡时间
     * @param timeout 格挡时长（毫秒）
     */
    fun setBlock(player: Player, timeout: Long)

    /**
     * 取消格挡
     */
    fun cancelBlock(player: Player)

    /**
     * 延长格挡时间
     * @param timeout 延长的格挡时长（毫秒）
     */
    fun addBlock(player: Player, timeout: Long)

    /**
     * 减少格挡时间
     * @param timeout 减少的格挡时长（毫秒）
     */
    fun reduceBlock(player: Player, timeout: Long)

    /**
     * 是否在沉默状态
     * @return 是否在沉默状态
     */
    fun isSilence(player: Player): Boolean

    /**
     * 获取沉默状态的倒计时
     * @return [Long] 毫秒
     */
    fun silenceCountdown(player: Player): Long

    /**
     * 设置沉默时间
     * @param timeout 沉默时长（毫秒）
     */
    fun setSilence(player: Player, timeout: Long)

    /**
     * 取消沉默
     */
    fun cancelSilence(player: Player)

    /**
     * 延长沉默时间
     * @param timeout 延长的沉默时长（毫秒）
     */
    fun addSilence(player: Player, timeout: Long)

    /**
     * 减少沉默时间
     * @param timeout 减少的沉默时长（毫秒）
     */
    fun reduceSilence(player: Player, timeout: Long)

}
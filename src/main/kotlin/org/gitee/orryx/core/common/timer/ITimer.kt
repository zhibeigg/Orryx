package org.gitee.orryx.core.common.timer

import org.gitee.orryx.core.kether.parameter.IParameter
import taboolib.common.platform.ProxyCommandSender

interface ITimer {

    /**
     * 重置计时
     * @param sender 计时者
     * @param parameter 计时参数
     * @return 计时时长（毫秒）
     * */
    fun reset(sender: ProxyCommandSender, parameter: IParameter): Long

    /**
     * 是否已完成冷却
     * @param sender 计时者
     * @param tag 计时名
     * */
    fun hasNext(sender: ProxyCommandSender, tag: String): Boolean

    /**
     * 获取倒计时
     * @param sender 计时者
     * @param tag 计时名
     * @return 倒计时毫秒
     * */
    fun getCountdown(sender: ProxyCommandSender, tag: String): Long

    /**
     * 增加计时时间
     * @param sender 计时者
     * @param tag 计时名
     * @param amount 毫秒
     * */
    fun increase(sender: ProxyCommandSender, tag: String, amount: Long)

    /**
     * 减少计时时间
     * @param sender 计时者
     * @param tag 计时名
     * @param amount 毫秒
     * */
    fun reduce(sender: ProxyCommandSender, tag: String, amount: Long)

    /**
     * 设置计时时间
     * @param sender 计时者
     * @param tag 计时名
     * @param amount 毫秒
     * */
    fun set(sender: ProxyCommandSender, tag: String, amount: Long)

    /**
     * 获取缓存
     * @param sender 计时者
     * @return 缓存的子计时器
     * */
    fun getCooldownMap(sender: ProxyCommandSender): MutableMap<String, CooldownEntry>

    /**
     * 获取缓存
     * @param sender 计时者
     * @return 缓存的子计时器
     * */
    fun getCooldownEntry(sender: ProxyCommandSender, tag: String): CooldownEntry?

}
package org.gitee.orryx.compat

import org.bukkit.entity.Player

/**
 * 按键注册发送器接口。
 *
 * 用于向客户端发送按键注册请求，隔离第三方插件依赖。
 */
interface IKeyRegisterSender {

    /**
     * 向玩家客户端发送按键注册。
     *
     * @param player 玩家
     * @param keys 需要注册的按键集合
     */
    fun sendKeyRegister(player: Player, keys: Set<String>)
}

package org.gitee.orryx.compat.arcartx

import org.bukkit.entity.Player
import org.gitee.orryx.compat.IKeyRegisterSender
import priv.seventeen.artist.arcartx.internal.network.NetworkMessageSender
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * ArcartX 按键注册发送器。
 */
@Ghost
class ArcartXKeyRegisterSender : IKeyRegisterSender {

    override fun sendKeyRegister(player: Player, keys: Set<String>) {
        try {
            NetworkMessageSender.sendPlayerJoinPacket(player)
        } catch (ex: Throwable) {
            warning("ArcartX按键同步失败: ${ex.message}")
        }
    }
}

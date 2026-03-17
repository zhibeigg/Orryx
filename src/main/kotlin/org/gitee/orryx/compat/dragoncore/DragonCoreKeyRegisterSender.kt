package org.gitee.orryx.compat.dragoncore

import org.bukkit.entity.Player
import org.gitee.orryx.compat.IKeyRegisterSender
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * DragonCore 按键注册发送器。
 */
@Ghost
class DragonCoreKeyRegisterSender : IKeyRegisterSender {

    override fun sendKeyRegister(player: Player, keys: Set<String>) {
        try {
            DragonCoreCustomPacketSender.sendKeyRegister(player, keys)
        } catch (ex: Throwable) {
            warning("DragonCore 按键注册失败: ${ex.message}")
        }
    }
}

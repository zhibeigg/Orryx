package org.gitee.orryx.compat.germplugin

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.KeyType
import org.bukkit.entity.Player
import org.gitee.orryx.compat.IKeyRegisterSender
import org.gitee.orryx.utils.MOUSE_LEFT
import org.gitee.orryx.utils.MOUSE_RIGHT
import taboolib.common.platform.Ghost
import taboolib.common.platform.function.warning

/**
 * GermPlugin 按键注册发送器。
 */
@Ghost
class GermKeyRegisterSender : IKeyRegisterSender {

    override fun sendKeyRegister(player: Player, keys: Set<String>) {
        keys.forEach {
            val key = when (it) {
                MOUSE_LEFT -> "MLEFT"
                MOUSE_RIGHT -> "MRIGHT"
                else -> it
            }
            try {
                GermPacketAPI.sendKeyRegister(player, KeyType.valueOf("KEY_${key}").keyId)
            } catch (ex: Throwable) {
                warning("GermPlugin 按键注册失败: ${ex.message}")
            }
        }
    }
}

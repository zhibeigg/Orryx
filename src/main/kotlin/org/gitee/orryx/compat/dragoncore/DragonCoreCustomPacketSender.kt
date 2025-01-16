package org.gitee.orryx.compat.dragoncore

import eos.moe.dragoncore.config.Config
import eos.moe.dragoncore.network.PacketBuffer
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.core.profile.IPlayerKeySetting
import taboolib.common.platform.Ghost
import java.util.*

@Ghost
object DragonCoreCustomPacketSender : PacketSender() {

    fun setPlayerAnimationController(player: Player, uuid: UUID, controllerYaml: String) {
        sendPluginMessage(player, 29) { buffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString(controllerYaml)
        }
    }

    fun removePlayerAnimationController(player: Player, uuid: UUID) {
        sendPluginMessage(player, 29) { buffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString("")
        }
    }

    fun runPlayerAnimationControllerFunction(player: Player, function: String) {
        sendPluginMessage(getNearPlayers(player), 36) { buffer ->
            buffer.writeUniqueId(player.uniqueId)
            buffer.writeString(function)
        }
    }

    fun sendKeyRegister(player: Player) {
        val list = IPlayerKeySetting.INSTANCE.let { listOf(it.aimConfirmKey(player), it.aimCancelKey(player)) }
        sendPluginMessage(player, 14) { buffer: PacketBuffer ->
            val set = (Config.fileMap["KeyConfig.yml"] as YamlConfiguration).getKeys(false)
            set.addAll(Config.registeredKeys + list)
            buffer.writeInt(set.size)
            val var2: Iterator<*> = set.iterator()
            while (var2.hasNext()) {
                val s = var2.next() as String
                buffer.writeString(s)
            }
        }
    }

}
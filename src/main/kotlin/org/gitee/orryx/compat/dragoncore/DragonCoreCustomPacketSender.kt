package org.gitee.orryx.compat.dragoncore

import eos.moe.dragoncore.config.Config
import eos.moe.dragoncore.network.PacketBuffer
import eos.moe.dragoncore.network.PacketSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.gitee.orryx.api.Orryx
import taboolib.common.platform.Ghost
import java.util.*

@Ghost
object DragonCoreCustomPacketSender : PacketSender() {

    fun setPlayerAnimationController(viewer: Player, uuid: UUID, controllerYaml: String) {
        sendPluginMessage(viewer, 29) { buffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString(controllerYaml)
        }
    }

    fun removePlayerAnimationController(viewer: Player, uuid: UUID) {
        sendPluginMessage(viewer, 29) { buffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString("")
        }
    }

    fun runPlayerAnimationControllerFunction(viewer: Player, uuid: UUID, function: String) {
        sendPluginMessage(viewer, 36) { buffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString(function)
        }
    }

    fun setPlayerAnimation(viewer: Player, uuid: UUID, animation: String, speed: Float) {
        sendPluginMessage(viewer, 27) { buffer: PacketBuffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString(animation)
            buffer.writeFloat(speed)
        }
    }

    fun removePlayerAnimation(viewer: Player, uuid: UUID) {
        sendPluginMessage(viewer, 28) { buffer: PacketBuffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString("all")
        }
    }

    fun removePlayerAnimation(viewer: Player, uuid: UUID, animation: String) {
        sendPluginMessage(viewer, 28) { buffer: PacketBuffer ->
            buffer.writeUniqueId(uuid)
            buffer.writeString(animation)
        }
    }

    fun setEntityModelItemAnimation(viewer: Player, entity: UUID, animation: String, speed: Float) {
        sendPluginMessage(viewer, 102) { buffer: PacketBuffer ->
            buffer.writeUniqueId(entity)
            buffer.writeString(animation)
            buffer.writeFloat(speed)
        }
    }

    fun sendKeyRegister(player: Player) {

        val list = Orryx.api().keyAPI.keySetting.let { listOf(it.aimConfirmKey(player), it.aimCancelKey(player)) }
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
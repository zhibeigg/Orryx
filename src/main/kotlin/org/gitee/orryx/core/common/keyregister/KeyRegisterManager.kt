package org.gitee.orryx.core.common.keyregister

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.KeyType
import eos.moe.dragoncore.api.event.EntityJoinWorldEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.DragonCorePlugin
import org.gitee.orryx.utils.GermPluginPlugin
import org.gitee.orryx.utils.keySetting
import org.gitee.orryx.utils.keySettingList
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.onlinePlayers
import java.util.*

object KeyRegisterManager {

    private val keyRegisterMap by unsafeLazy { hashMapOf<UUID, KeyRegister>() }

    @SubscribeEvent
    private fun onJoin(e: PlayerJoinEvent) {
        keyRegisterMap[e.player.uniqueId] = KeyRegister(e.player)
    }

    @SubscribeEvent
    private fun onQuit(e: PlayerQuitEvent) {
        keyRegisterMap.remove(e.player.uniqueId)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private fun onPlayerJoin(e: EntityJoinWorldEvent) {
        sendKeyRegister(e.player)
    }

    @Reload(2)
    private fun reload() {
        onlinePlayers.forEach {
            sendKeyRegister(it)
        }
    }

    private fun sendKeyRegister(player: Player) {
        player.keySetting { keySetting ->
            if (GermPluginPlugin.isEnabled) {
                keySetting.keySettingList().forEach {
                    try {
                        GermPacketAPI.sendKeyRegister(player, KeyType.valueOf("KEY_${it}").keyId)
                    } catch (ex: Throwable) {
                        warning("GermPlugin 按键注册失败: ${ex.message}")
                    }
                }
            }
            if (DragonCorePlugin.isEnabled) {
                try {
                    DragonCoreCustomPacketSender.sendKeyRegister(player, keySetting.keySettingList())
                } catch (ex: Throwable) {
                    warning("DragonCore按键注册失败: ${ex.message}")
                }
            }
        }
    }

    fun getKeyRegister(uuid: UUID): IKeyRegister? {
        return keyRegisterMap[uuid]
    }

}
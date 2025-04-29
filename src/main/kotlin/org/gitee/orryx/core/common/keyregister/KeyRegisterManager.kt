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
import org.gitee.orryx.utils.MOUSE_LEFT
import org.gitee.orryx.utils.MOUSE_RIGHT
import org.gitee.orryx.utils.keySetting
import org.gitee.orryx.utils.keySettingSet
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.onlinePlayers
import java.util.*

object KeyRegisterManager {

    private val keyRegisterMap by unsafeLazy { hashMapOf<UUID, KeyRegister>() }
    private val players = hashSetOf<UUID>()

    @SubscribeEvent
    private fun onJoin(e: PlayerJoinEvent) {
        players.add(e.player.uniqueId)
        keyRegisterMap[e.player.uniqueId] = KeyRegister(e.player)
    }

    @SubscribeEvent
    private fun onQuit(e: PlayerQuitEvent) {
        keyRegisterMap.remove(e.player.uniqueId)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    private fun onPlayerJoin(e: EntityJoinWorldEvent) {
        if (players.remove(e.player.uniqueId)) {
            sendKeyRegister(e.player)
        }
    }

    @Reload(2)
    private fun reload() {
        onlinePlayers.forEach {
            sendKeyRegister(it)
        }
    }

    private fun sendKeyRegister(player: Player) {
        player.keySetting { keySetting ->
            when {
                GermPluginPlugin.isEnabled -> {
                    keySetting.keySettingSet().forEach {
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
                DragonCorePlugin.isEnabled -> {
                    try {
                        DragonCoreCustomPacketSender.sendKeyRegister(player, keySetting.keySettingSet())
                    } catch (ex: Throwable) {
                        warning("DragonCore按键注册失败: ${ex.message}")
                    }
                }
            }
        }
    }

    fun getKeyRegister(uuid: UUID): IKeyRegister? {
        return keyRegisterMap[uuid]
    }
}
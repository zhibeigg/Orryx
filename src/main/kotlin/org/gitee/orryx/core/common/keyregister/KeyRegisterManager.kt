package org.gitee.orryx.core.common.keyregister

import com.germ.germplugin.api.GermPacketAPI
import com.germ.germplugin.api.KeyType
import com.germ.germplugin.api.event.GermClientLinkedEvent
import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.events.compat.DragonCacheLoadedEvent
import org.gitee.orryx.compat.dragoncore.DragonCoreCustomPacketSender
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.utils.*
import taboolib.common.platform.Ghost
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
        if (!DragonCorePlugin.isEnabled && !GermPluginPlugin.isEnabled) return
        players.add(e.player.uniqueId)
        keyRegisterMap[e.player.uniqueId] = KeyRegister(e.player)
    }

    @SubscribeEvent
    private fun onQuit(e: PlayerQuitEvent) {
        keyRegisterMap.remove(e.player.uniqueId)
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOW)
    private fun onPlayerJoin(e: DragonCacheLoadedEvent) {
        if (players.remove(e.player.uniqueId)) {
            sendKeyRegister(e.player)
        }
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOW)
    private fun callDragonCacheLoadedEvent(e: CustomPacketEvent) {
        if (e.identifier == "DragonCore" && e.data.size == 1 && e.data[0] == "cache_loaded") {
            DragonCacheLoadedEvent(e.player).call()
        }
    }

    @Ghost
    @SubscribeEvent(priority = EventPriority.LOW)
    private fun onPlayerJoin(e: GermClientLinkedEvent) {
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
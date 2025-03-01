package org.gitee.orryx.core.common.keyregister

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import java.util.*

object KeyRegisterManager {

    private val keyRegisterMap by lazy { hashMapOf<UUID, KeyRegister>() }

    @SubscribeEvent
    private fun onJoin(e: PlayerJoinEvent) {
        keyRegisterMap[e.player.uniqueId] = KeyRegister(e.player)
    }

    @SubscribeEvent
    private fun onQuit(e: PlayerQuitEvent) {
        keyRegisterMap.remove(e.player.uniqueId)
    }

    fun getKeyRegister(uuid: UUID): IKeyRegister? {
        return keyRegisterMap[uuid]
    }

}
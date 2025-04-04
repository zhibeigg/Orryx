package org.gitee.orryx.module.state

import eos.moe.dragoncore.api.event.KeyPressEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.core.common.keyregister.KeyRegisterManager
import org.gitee.orryx.utils.MOUSE_LEFT
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.warning
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptService
import java.util.*

object StateManager {

    private val playerDataMap = hashMapOf<UUID, PlayerData>()

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        playerDataMap.remove(e.player.uniqueId)
    }

    @Ghost
    @SubscribeEvent
    private fun press(e: KeyPressEvent) {
        val data = playerDataMap.getOrPut(e.player.uniqueId) { PlayerData(e.player) }
        if (data.nowRunningState?.state?.hasNext(e.key) == false) {
            data.nextInput = e.key
        } else {
            data.next(e.key)
        }
    }

    fun callNext(player: Player): IRunningState? {
        val data = playerDataMap.getOrPut(player.uniqueId) { PlayerData(player) }
        return data.nextInput?.let { data.next(it) } ?: run {
            if (KeyRegisterManager.getKeyRegister(player.uniqueId)?.isKeyPress(MOUSE_LEFT) == true) {
                data.next(MOUSE_LEFT)
            } else {
                null
            }
        }
    }

    fun loadScript(state: IActionState, action: String): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, state.key, getBytes(action), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            ex.printStackTrace()
            warning("State: ${state.key}")
            null
        }
    }

}
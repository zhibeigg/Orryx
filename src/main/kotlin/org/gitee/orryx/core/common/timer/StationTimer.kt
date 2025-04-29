package org.gitee.orryx.core.common.timer

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.utils.getBaffle
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.util.unsafeLazy
import java.util.*

object StationTimer: ITimer {

    private val playerCooldowns by unsafeLazy { hashMapOf<UUID, MutableMap<String, CooldownEntry>>() }

    override fun reset(sender: ProxyCommandSender, parameter: IParameter): Long {
        require(parameter is StationParameter<*>) { "Invalid parameter type" }
        val timeout = parameter.getStation().getBaffle(sender, parameter)
        val stationName = parameter.stationLoader

        if (timeout > 0) {
            set(sender, stationName, timeout)
        } else {
            getCooldownMap(sender).remove(stationName)
        }
        return timeout
    }

    override fun hasNext(sender: ProxyCommandSender, tag: String): Boolean {
        return getCooldownMap(sender)[tag]?.isReady ?: true
    }

    override fun getCountdown(sender: ProxyCommandSender, tag: String): Long {
        return getCooldownMap(sender)[tag]?.countdown ?: 0
    }

    override fun increase(sender: ProxyCommandSender, tag: String, amount: Long) {
        getCooldownMap(sender)[tag]?.addDuration(amount)
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        getCooldownMap(sender)[tag]?.reduceDuration(amount)
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        val cooldownMap = getCooldownMap(sender)
        cooldownMap.values.removeIf { it.isReady }
        cooldownMap[tag] = CooldownEntry(tag, amount)
    }

    override fun getCooldownMap(sender: ProxyCommandSender): MutableMap<String, CooldownEntry> {
        val playerId = sender.castSafely<Player>()?.uniqueId ?: throw IllegalArgumentException("Sender must be a Player")
        return playerCooldowns.getOrPut(playerId) { hashMapOf() }
    }

    override fun getCooldownEntry(sender: ProxyCommandSender, tag: String): CooldownEntry? {
        return getCooldownMap(sender)[tag]
    }

    @SubscribeEvent
    private fun onPlayerQuit(e: PlayerQuitEvent) {
        playerCooldowns.remove(e.player.uniqueId)
    }
}
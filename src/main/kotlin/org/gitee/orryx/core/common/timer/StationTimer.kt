package org.gitee.orryx.core.common.timer

import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.common.Baffle
import org.gitee.orryx.core.kether.parameter.IParameter
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.station.stations.StationLoaderManager
import org.gitee.orryx.utils.getBaffle
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.SubscribeEvent

object StationTimer: ITimer {

    private val map = mutableMapOf<String, MutableMap<String, Baffle>>()

    override fun reset(sender: ProxyCommandSender, parameter: IParameter): Long {
        parameter as StationParameter
        StationLoaderManager.getStationLoader(parameter.stationLoader)?.let {
            val timeout = it.getBaffle(sender, parameter)
            if (timeout > 0) {
                set(sender, parameter.stationLoader,  timeout)
            } else {
                getCache(sender).remove(parameter.stationLoader)
            }
            return timeout
        }
        return 0
    }

    override fun hasNext(sender: ProxyCommandSender, tag: String): Boolean {
        return getCache(sender)[tag]?.next ?: true
    }

    override fun getCountdown(sender: ProxyCommandSender, tag: String): Long {
        return getCache(sender)[tag]?.countdown ?: 0
    }

    override fun increase(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffle = getCache(sender)[tag] ?: return
        baffle.increase(amount)
    }

    override fun reduce(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffle = getCache(sender)[tag] ?: return
        baffle.reduce(amount)
    }

    override fun set(sender: ProxyCommandSender, tag: String, amount: Long) {
        val baffles = getCache(sender)
        val iterator = baffles.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val value = entry.value
            if (value.next) {
                iterator.remove()
            }
        }
        baffles[tag] = Baffle(tag, amount)
    }

    override fun getCache(sender: ProxyCommandSender) = map.computeIfAbsent(sender.name) { mutableMapOf() }

    @SubscribeEvent
    private fun quit(e: PlayerQuitEvent) {
        map.remove(e.player.name)
    }

}
package org.gitee.orryx.core.station.stations

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.gitee.orryx.api.OrryxAPI
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import org.gitee.orryx.core.common.timer.StationTimer
import org.gitee.orryx.core.kether.PlayerRunningSpace
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.station.TriggerManager
import org.gitee.orryx.utils.files
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import taboolib.common.platform.function.warning
import taboolib.common.util.unsafeLazy
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService

object StationLoaderManager {

    private val stationMap by unsafeLazy { hashMapOf<String, IStation>() }
    private val listenerList by unsafeLazy { mutableListOf<ProxyListener>() }

    internal fun getStationLoader(stationLoader: String): IStation? {
        return stationMap[stationLoader]
    }

    internal fun getStationLoaders(): Map<String, IStation> {
        return stationMap
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        listenerList.forEach { unregisterListener(it) }
        listenerList.clear()
        stationMap.clear()
        files("stations", "example.yml") { file ->
            val configuration = Configuration.loadFromFile(file)
            val station = StationLoader(file.nameWithoutExtension, configuration)
            stationMap[station.key] = station
        }
        autoRegister()
        info("&e┣&7Triggers loaded &e${TriggerManager.stationTriggersMap.size} &a√".colored())
        info("&e┣&7Stations loaded &e${stationMap.size} &a√".colored())
    }

    internal fun loadScript(station: StationLoader): Script? {
        return try {
            OrryxAPI.ketherScriptLoader.load(ScriptService, station.key, getBytes(station.actions), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            ex.printStackTrace()
            warning("Station: ${station.configuration.file}")
            null
        }
    }

    private fun autoRegister() {
        val events = stationMap.map { it.value.event }.distinct()
        events.forEach { event ->
            val trigger = TriggerManager.stationTriggersMap[event] ?: return@forEach
            val list = stationMap.filter { it.value.event == event }.values
            list.groupBy { it.priority }.forEach { (priority, sub) ->
                sub.sortedByDescending { it.weight }.also {
                    try {
                        trigger.register(priority, it)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    private fun <E> IStationTrigger<E>.register(priority: EventPriority, stations: List<IStation>) {
        listenerList += registerBukkitListener(clazz, priority, false) { event ->
            stations.forEach { station ->
                val map = specialKeys.associateWith { (station as? StationLoader)?.options?.get(it) }
                if (onCheck(station, event, map)) {
                    val sender = onJoin(event, map)
                    if (StationTimer.hasNext(sender, station.key)) {
                        val parameter = StationParameter(station.key, sender, event)
                        StationTimer.reset(sender, parameter)
                        startStation(sender, station, map, event, parameter)
                    }
                }
            }
        }
    }

    private fun <E> IStationTrigger<E>.startStation(sender: ProxyCommandSender, station: IStation, map: Map<String, Any?>, event: E, parameter: StationParameter<E>) {
        lateinit var context: ScriptContext
        val player = sender.castSafely<Player>()
        fun run() {
            val playerRunningSpace =
                if (player != null) {
                    ScriptManager.runningStationScriptsMap.getOrPut(player.uniqueId) { PlayerRunningSpace(player) }
                } else {
                    null
                }

            ScriptManager.runScript(sender, parameter, station.script ?: error("请修复中转站${station.key}的脚本配置")) {
                context = this
                onStart(this, event, map)
                playerRunningSpace?.invoke(context, station.key)
            }.whenComplete { _, _ ->
                onEnd(context, event, map)
                playerRunningSpace?.release(context, station.key)
            }
        }

        if (station.async) {
            pluginScope.launch { run() }
        } else {
            run()
        }
    }
}
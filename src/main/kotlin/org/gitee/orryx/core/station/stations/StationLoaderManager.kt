package org.gitee.orryx.core.station.stations

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.gitee.orryx.api.OrryxAPI.ketherScriptLoader
import org.gitee.orryx.core.common.timer.StationTimer
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.reload.Reload
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.utils.debug
import org.gitee.orryx.utils.files
import org.gitee.orryx.utils.getBytes
import org.gitee.orryx.utils.orryxEnvironmentNamespaces
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.info
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import taboolib.common.platform.function.warning
import taboolib.library.reflex.ReflexClass
import taboolib.module.chat.colored
import taboolib.module.configuration.Configuration
import taboolib.module.kether.Script
import taboolib.module.kether.ScriptContext
import taboolib.module.kether.ScriptService
import taboolib.module.kether.printKetherErrorMessage

@Awake
object StationLoaderManager: ClassVisitor(1) {

    private val triggers by lazy { mutableMapOf<String, IStationTrigger<*>>() }
    private val stationMap by lazy { mutableMapOf<String, IStation>() }
    private val listenerList by lazy { mutableListOf<ProxyListener>() }

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.INIT
    }

    internal fun getStationLoader(stationLoader: String): IStation? {
        return stationMap[stationLoader]
    }

    @Reload(1)
    @Awake(LifeCycle.ENABLE)
    private fun reload() {
        listenerList.forEach { unregisterListener(it) }
        listenerList.clear()
        stationMap.clear()
        files("stations", "example.yml") { file ->
            val configuration = Configuration.loadFromFile(file)
            val station = StationLoader(configuration.name, configuration)
            stationMap[station.key] = station
        }
        autoRegister()
        info("&e┣&7Triggers loaded &e${triggers.size} &a√".colored())
        info("&e┣&7Stations loaded &e${stationMap.size} &a√".colored())
    }

    internal fun loadScript(station: StationLoader): Script? {
        return try {
            ketherScriptLoader.load(ScriptService, station.key, getBytes(station.actions), orryxEnvironmentNamespaces)
        } catch (ex: Exception) {
            ex.printKetherErrorMessage()
            warning("Station: ${station.configuration.file}")
            null
        }
    }

    override fun visitStart(clazz: ReflexClass) {
        if (IStationTrigger::class.java.isAssignableFrom(clazz.toClass())) {
            val instance = clazz.getInstance() as? IStationTrigger<*> ?: return
            if (clazz.hasAnnotation(Plugin::class.java)) {
                val annotation = clazz.getAnnotation(Plugin::class.java)
                val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                debug("&e┣&7StationTrigger loaded &e${instance.event} ${if (pluginEnabled) "&a√" else "&4×"}")
                if (!pluginEnabled) return
            } else {
                debug("&e┣&7StationTrigger loaded &e${instance.event} &a√")
            }
            triggers[instance.event] = instance
        }
    }

    private fun autoRegister() {
        val events = stationMap.map { it.value.event }.distinct()
        events.forEach { event ->
            val trigger = triggers[event] ?: return@forEach
            val list = stationMap.filter { it.value.event == event }.values
            list.groupBy { it.priority }.forEach { (priority, sub) ->
                sub.sortedByDescending { it.weight }.also {
                    trigger.register(priority, it)
                }
            }
        }
    }

    private fun <E : Event> IStationTrigger<E>.register(priority: EventPriority, stations: List<IStation>) {
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

    private fun <E : Event> IStationTrigger<E>.startStation(sender: ProxyCommandSender, station: IStation, map: Map<String, Any?>, event: E, parameter: StationParameter) {
        lateinit var context: ScriptContext
        ScriptManager.runScript(sender, parameter, station.script ?: error("请修复中转站${station.key}的脚本配置")) {
            context = this
            onStart(this, event, map)
        }.thenRun {
            onEnd(context, event, map)
        }
    }

}
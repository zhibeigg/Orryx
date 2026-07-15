package org.gitee.orryx.core.station

import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.gitee.orryx.api.events.register.OrryxTriggerRegisterEvent
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import org.gitee.orryx.core.station.pipe.PipeTriggerKey
import org.gitee.orryx.core.station.stations.IStationTrigger
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.reflect.hasAnnotation
import taboolib.library.reflex.ReflexClass
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Awake
object TriggerManager: ClassVisitor(3) {

    private val pipeTriggers = ConcurrentHashMap<String, IPipeTrigger<*>>()
    private val stationTriggers = ConcurrentHashMap<String, IStationTrigger<*>>()

    val pipeTriggersMap: Map<String, IPipeTrigger<*>>
        get() = pipeTriggers

    val stationTriggersMap: Map<String, IStationTrigger<*>>
        get() = stationTriggers

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.LOAD
    }

    override fun visitStart(clazz: ReflexClass) {
        val c = clazz.toClass()
        if (WikiTrigger::class.java.isAssignableFrom(c)) {
            try {
                val instance = clazz.getInstance() as? WikiTrigger
                if (instance != null) {
                    val wiki = instance.wiki
                    if (instance is ITrigger<*>) {
                        wiki.event(instance.clazz, Cancellable::class.java.isAssignableFrom(instance.clazz))
                    }
                    ScriptManager.wikiTriggers.add(wiki)
                }
            } catch (_: Throwable){
                return
            }
        }
        if (IPipeTrigger::class.java.isAssignableFrom(c)) {
            val instance = try {
                clazz.getInstance() as? IPipeTrigger<*> ?: return
            } catch (_: Throwable) {
                return
            }
            if (clazz.hasAnnotation(Plugin::class.java)) {
                val annotation = clazz.getAnnotation(Plugin::class.java)
                val pluginLoaded = Bukkit.getPluginManager().getPlugin(annotation.property<String>("plugin")!!) != null
                debug { "&e┣&7PipeTrigger loaded &e${instance.event} ${if (pluginLoaded) "&a√" else "&4×"}" }
                if (!pluginLoaded) return
            } else {
                debug { "&e┣&7PipeTrigger loaded &e${instance.event} &a√" }
            }
            pipeTriggers[PipeTriggerKey.normalize(instance.event)] = instance
        }
        if (IStationTrigger::class.java.isAssignableFrom(c)) {
            val instance = try {
                clazz.getInstance() as? IStationTrigger<*> ?: return
            } catch (_: Throwable) {
                return
            }
            if (clazz.hasAnnotation(Plugin::class.java)) {
                val annotation = clazz.getAnnotation(Plugin::class.java)
                val pluginLoaded = Bukkit.getPluginManager().getPlugin(annotation.property<String>("plugin")!!) != null
                debug { "&e┣&7StationTrigger loaded &e${instance.event} ${if (pluginLoaded) "&a√" else "&4×"}" }
                if (!pluginLoaded) return
            } else {
                debug { "&e┣&7StationTrigger loaded &e${instance.event} &a√" }
            }
            stationTriggers[instance.event.trim().uppercase(Locale.ROOT)] = instance
        }
    }

    @Awake(LifeCycle.ENABLE)
    private fun pluginRegister() {
        val event = OrryxTriggerRegisterEvent()
        event.call()
        event.list().forEach {
            val clazz = it.javaClass
            if (it is IPipeTrigger) {
                val pluginLoaded = !clazz.hasAnnotation(Plugin::class.java) || Bukkit.getPluginManager()
                    .getPlugin(clazz.getAnnotation(Plugin::class.java).plugin) != null
                if (pluginLoaded) {
                    pipeTriggers[PipeTriggerKey.normalize(it.event)] = it
                }
                consoleMessage("&e┣&c第三方 &7PipeTrigger loaded &e${it.event} ${if (pluginLoaded) "&a√" else "&4×"}")
            }
            if (it is IStationTrigger) {
                val pluginLoaded = !clazz.hasAnnotation(Plugin::class.java) || Bukkit.getPluginManager()
                    .getPlugin(clazz.getAnnotation(Plugin::class.java).plugin) != null
                if (pluginLoaded) {
                    stationTriggers[it.event.trim().uppercase(Locale.ROOT)] = it
                }
                consoleMessage("&e┣&c第三方 &7PluginStationTrigger loaded &e${it.event} ${if (pluginLoaded) "&a√" else "&4×"}")
            }
        }
    }
}
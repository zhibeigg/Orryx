package org.gitee.orryx.core.station

import org.bukkit.Bukkit
import org.gitee.orryx.api.events.register.OrryxTriggerRegisterEvent
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import org.gitee.orryx.core.station.stations.IStationTrigger
import org.gitee.orryx.utils.consoleMessage
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
import taboolib.common.reflect.hasAnnotation
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.ReflexClass

@Awake
object TriggerManager: ClassVisitor(3) {

    private val pipeTriggers by unsafeLazy { hashMapOf<String, IPipeTrigger<*>>() }
    private val stationTriggers by unsafeLazy { hashMapOf<String, IStationTrigger<*>>() }

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
                (clazz.getInstance() as? WikiTrigger)?.also { ScriptManager.wikiTriggers.add(it.wiki) }
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
                debug("&e┣&7PipeTrigger loaded &e${instance.event} ${if (pluginLoaded) "&a√" else "&4×"}")
                if (!pluginLoaded) return
            } else {
                debug("&e┣&7PipeTrigger loaded &e${instance.event} &a√")
            }
            pipeTriggers[instance.event.uppercase()] = instance
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
                debug("&e┣&7StationTrigger loaded &e${instance.event} ${if (pluginLoaded) "&a√" else "&4×"}")
                if (!pluginLoaded) return
            } else {
                debug("&e┣&7StationTrigger loaded &e${instance.event} &a√")
            }
            stationTriggers[instance.event.uppercase()] = instance
        }
    }

    @Awake(LifeCycle.ENABLE)
    private fun pluginRegister() {
        val event = OrryxTriggerRegisterEvent()
        event.call()
        event.list().forEach {
            val clazz = it.javaClass
            if (it is IPipeTrigger) {
                if (clazz.hasAnnotation(Plugin::class.java)) {
                    val annotation = clazz.getAnnotation(Plugin::class.java)
                    val pluginLoaded = Bukkit.getPluginManager().getPlugin(annotation.plugin) != null
                    if (pluginLoaded) {
                        pipeTriggers[it.event.uppercase()] = it
                    }
                    consoleMessage("&e┣&c第三方 &7PipeTrigger loaded &e${it.event} ${if (pluginLoaded) "&a√" else "&4×"}")
                }
            }
            if (it is IStationTrigger) {
                if (clazz.hasAnnotation(Plugin::class.java)) {
                    val annotation = clazz.getAnnotation(Plugin::class.java)
                    val pluginLoaded = Bukkit.getPluginManager().getPlugin(annotation.plugin) != null
                    if (pluginLoaded) {
                        stationTriggers[it.event.uppercase()] = it
                    }
                    consoleMessage("&e┣&c第三方 &7PluginStationTrigger loaded &e${it.event} ${if (pluginLoaded) "&a√" else "&4×"}")
                }
            }
        }
    }
}
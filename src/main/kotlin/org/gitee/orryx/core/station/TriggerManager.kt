package org.gitee.orryx.core.station

import org.bukkit.Bukkit
import org.gitee.orryx.core.kether.ScriptManager
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import org.gitee.orryx.core.station.stations.IStationTrigger
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.Awake
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
        return LifeCycle.INIT
    }

    override fun visitStart(clazz: ReflexClass) {
        val c = clazz.toClass()
        if (WikiTrigger::class.java.isAssignableFrom(c)) {
            (clazz.getInstance() as? WikiTrigger)?.also { ScriptManager.wikiTriggers.add(it.wiki) }
        }
        if (IPipeTrigger::class.java.isAssignableFrom(c)) {
            val instance = try {
                clazz.getInstance() as? IPipeTrigger<*> ?: return
            } catch (_: Throwable) {
                return
            }
            if (clazz.hasAnnotation(Plugin::class.java)) {
                val annotation = clazz.getAnnotation(Plugin::class.java)
                val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                debug("&e┣&7PipeTrigger loaded &e${instance.event} ${if (pluginEnabled) "&a√" else "&4×"}")
                if (!pluginEnabled) return
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
                val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                debug("&e┣&7StationTrigger loaded &e${instance.event} ${if (pluginEnabled) "&a√" else "&4×"}")
                if (!pluginEnabled) return
            } else {
                debug("&e┣&7StationTrigger loaded &e${instance.event} &a√")
            }
            stationTriggers[instance.event.uppercase()] = instance
        }
    }

}
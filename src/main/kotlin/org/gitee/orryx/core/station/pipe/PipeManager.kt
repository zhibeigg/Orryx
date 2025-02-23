package org.gitee.orryx.core.station.pipe

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.utils.debug
import taboolib.common.LifeCycle
import taboolib.common.inject.ClassVisitor
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import taboolib.library.reflex.ReflexClass
import java.util.*

object PipeManager: ClassVisitor(1) {

    private val triggers = mutableMapOf<String, IPipeTrigger<*>>()
    private val pipeTaskMap = mutableMapOf<UUID, IPipeTask>()

    override fun getLifeCycle(): LifeCycle {
        return LifeCycle.ENABLE
    }

    override fun visitStart(clazz: ReflexClass) {
        if (IPipeTrigger::class.java.isAssignableFrom(clazz.toClass())) {
            val instance = clazz.getInstance() as? IPipeTrigger<*> ?: return
            if (clazz.hasAnnotation(Plugin::class.java)) {
                val annotation = clazz.getAnnotation(Plugin::class.java)
                val pluginEnabled = Bukkit.getPluginManager().isPluginEnabled(annotation.property<String>("plugin")!!)
                debug("&e┣&7PipeTrigger loaded &e${instance.event} ${if (pluginEnabled) "&a√" else "&4×"}")
                if (!pluginEnabled) return
            } else {
                debug("&e┣&7PipeTrigger loaded &e${instance.event} &a√")
            }
            triggers[instance.event] = instance
        }
    }

    fun addPipeTask(task: IPipeTask) {
        task.brokeTriggers.forEach {
            val trigger = triggers[it] ?: error("PipeTask UUID: ${task.uuid}发现写入不存在的trigger: $it")
            registerListener(trigger)
        }
        pipeTaskMap[task.uuid] = task
    }

    private fun <E : Event> registerListener(trigger: IPipeTrigger<E>) {
        if (trigger.listener == null) {
            trigger.listener = registerBukkitListener(trigger.clazz) { event ->
                val iterator = pipeTaskMap.values.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (trigger.event in next.brokeTriggers && trigger.onCheck(next, event, emptyMap())) {
                        trigger.onJoin(event, emptyMap())
                        next.scriptContext?.let { trigger.onStart(it, event, emptyMap()) }
                        next.broke().thenRun {
                            next.scriptContext?.let { trigger.onEnd(it, event, emptyMap()) }
                        }
                    }
                }
            }
        }
    }

    fun getPipeTask(uuid: UUID): IPipeTask? {
        return pipeTaskMap[uuid]
    }

    fun removePipeTask(pipeTask: IPipeTask) {
        pipeTaskMap.remove(pipeTask.uuid)
        checkListener()
    }

    private fun checkListener() {
        val triggers = pipeTaskMap.flatMap { it.value.brokeTriggers }.distinct()
        this.triggers.filter {
            it.key !in triggers
        }.forEach { (_, listener) ->
            listener.listener?.also {
                unregisterListener(it)
                listener.listener = null
            }
        }
    }

}
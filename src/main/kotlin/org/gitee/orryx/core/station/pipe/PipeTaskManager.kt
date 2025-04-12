package org.gitee.orryx.core.station.pipe

import org.gitee.orryx.core.station.TriggerManager
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import java.util.*

object PipeTaskManager {

    private val pipeTaskMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { hashMapOf<UUID, IPipeTask>() }

    fun addPipeTask(task: IPipeTask) {
        task.brokeTriggers.forEach {
            val trigger = TriggerManager.pipeTriggersMap[it] ?: error("PipeTask UUID: ${task.uuid}发现写入不存在的trigger: $it")
            registerListener(trigger)
        }
        pipeTaskMap[task.uuid] = task
    }

    private fun <E> registerListener(trigger: IPipeTrigger<E>) {
        if (trigger.listener == null) {
            trigger.listener = registerBukkitListener(trigger.clazz) { event ->
                val iterator = pipeTaskMap.values.iterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    if (trigger.event in next.brokeTriggers && trigger.onCheck(next, event, emptyMap())) {
                        next.scriptContext?.let { trigger.onStart(it, event, emptyMap()) }
                        next.broke().whenComplete { _, _ ->
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
        TriggerManager.pipeTriggersMap.filter {
            it.key !in triggers
        }.forEach { (_, listener) ->
            listener.listener?.also {
                unregisterListener(it)
                listener.listener = null
            }
        }
    }

}
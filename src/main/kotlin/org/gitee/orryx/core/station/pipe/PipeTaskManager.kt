package org.gitee.orryx.core.station.pipe

import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import org.gitee.orryx.core.station.TriggerManager
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.unregisterListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PipeTaskManager {

    private val pipeTaskMap = ConcurrentHashMap<UUID, IPipeTask>()

    fun addPipeTask(task: IPipeTask) {
        task.brokeTriggers.forEach { triggerKey ->
            pluginScope.launch {
                val trigger = TriggerManager.pipeTriggersMap[triggerKey] ?: error("PipeTask UUID: ${task.uuid}发现写入不存在的trigger: $triggerKey")
                if (trigger.listener == null) {
                    synchronized(trigger) {
                        if (trigger.listener == null) {
                            trigger.listener = registerBukkitListener(trigger)
                        }
                    }
                }
            }
        }
        pipeTaskMap[task.uuid] = task
    }

    private fun <E> registerBukkitListener(trigger: IPipeTrigger<E>): ProxyListener {
        return registerBukkitListener(trigger.clazz) { event ->
            pipeTaskMap.values.forEach { pipeTask ->
                if (trigger.event in pipeTask.brokeTriggers && trigger.onCheck(pipeTask, event, pipeTask.scriptContext?.rootFrame()?.variables()?.toMap() ?: emptyMap())) {
                    pipeTask.scriptContext?.let { trigger.onStart(it, event, pipeTask.scriptContext?.rootFrame()?.variables()?.toMap() ?: emptyMap()) }
                    pipeTask.broke().whenComplete { _, _ ->
                        pipeTask.scriptContext?.let { trigger.onEnd(it, event, pipeTask.scriptContext?.rootFrame()?.variables()?.toMap() ?: emptyMap()) }
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
        val activeTriggers = pipeTaskMap.values.flatMap { it.brokeTriggers }.toSet()
        TriggerManager.pipeTriggersMap.values.forEach { trigger ->
            pluginScope.launch {
                synchronized(trigger) {
                    // 再次检查是否有任务使用该Trigger
                    val isStillInactive = activeTriggers.none { it == trigger.event }
                    if (isStillInactive && trigger.listener != null) {
                        unregisterListener(trigger.listener!!)
                        trigger.listener = null
                    }
                }
            }
        }
    }
}
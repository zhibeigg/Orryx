package org.gitee.orryx.core.station.pipe

import kotlinx.coroutines.launch
import org.gitee.orryx.api.OrryxAPI.Companion.pluginScope
import org.gitee.orryx.core.station.TriggerManager
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.unregisterListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PipeTaskManager {

    private val pipeTaskMap = ConcurrentHashMap<UUID, IPipeTask>()

    /**
     * 按触发器分组的任务索引，用于快速查找
     * key: 触发器事件名, value: 使用该触发器的任务UUID集合
     */
    private val triggerTaskIndex = ConcurrentHashMap<String, MutableSet<UUID>>()

    fun addPipeTask(task: IPipeTask) {
        task.brokeTriggers.forEach { triggerKey ->
            // 更新索引
            triggerTaskIndex.getOrPut(triggerKey) { ConcurrentHashMap.newKeySet() }.add(task.uuid)

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
            // 使用索引快速获取相关任务，避免遍历所有任务
            val taskUuids = triggerTaskIndex[trigger.event] ?: return@registerBukkitListener
            taskUuids.forEach { uuid ->
                val pipeTask = pipeTaskMap[uuid] ?: return@forEach
                if (trigger.onCheck(pipeTask, event, pipeTask.scriptContext?.rootFrame()?.variables()?.toMap() ?: emptyMap())) {
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
        // 从索引中移除
        pipeTask.brokeTriggers.forEach { triggerKey ->
            triggerTaskIndex[triggerKey]?.remove(pipeTask.uuid)
        }
        checkListener()
    }

    private fun checkListener() {
        TriggerManager.pipeTriggersMap.values.forEach { trigger ->
            pluginScope.launch {
                synchronized(trigger) {
                    // 使用索引检查是否有任务使用该Trigger
                    val taskUuids = triggerTaskIndex[trigger.event]
                    val isInactive = taskUuids.isNullOrEmpty()
                    if (isInactive && trigger.listener != null) {
                        unregisterListener(trigger.listener!!)
                        trigger.listener = null
                        // 清理空的索引条目
                        triggerTaskIndex.remove(trigger.event)
                    }
                }
            }
        }
    }
}
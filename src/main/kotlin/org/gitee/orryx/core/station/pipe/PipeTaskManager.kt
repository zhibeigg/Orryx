package org.gitee.orryx.core.station.pipe

import org.gitee.orryx.core.station.TriggerManager
import org.gitee.orryx.utils.runOnMainThread
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.registerBukkitListener
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.unregisterListener
import taboolib.common.platform.function.warning
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PipeTaskManager {

    private val pipeTaskMap = ConcurrentHashMap<UUID, IPipeTask>()
    private val triggerTaskIndex = ConcurrentHashMap<String, MutableSet<UUID>>()
    private val listenerLock = Any()

    fun addPipeTask(task: IPipeTask) {
        val triggerKeys = task.brokeTriggers.map(PipeTriggerKey::normalize)
        val missing = triggerKeys.firstOrNull { TriggerManager.pipeTriggersMap[it] == null }
        require(missing == null) { "PipeTask UUID: ${task.uuid} 使用了不存在的 trigger: $missing" }
        require(pipeTaskMap.putIfAbsent(task.uuid, task) == null) { "重复的 PipeTask UUID: ${task.uuid}" }

        triggerKeys.forEach { triggerKey ->
            triggerTaskIndex.computeIfAbsent(triggerKey) { ConcurrentHashMap.newKeySet() }.add(task.uuid)
            ensureListener(triggerKey)
        }
    }

    private fun ensureListener(triggerKey: String) {
        onMainThread {
            synchronized(listenerLock) {
                val trigger = TriggerManager.pipeTriggersMap[triggerKey] ?: return@synchronized
                if (triggerTaskIndex[triggerKey].isNullOrEmpty() || trigger.listener != null) return@synchronized
                trigger.listener = registerBukkitListener(triggerKey, trigger)
            }
        }
    }

    private fun <E> registerBukkitListener(triggerKey: String, trigger: IPipeTrigger<E>): ProxyListener {
        return registerBukkitListener(trigger.clazz) { event ->
            val taskUuids = triggerTaskIndex[triggerKey]?.toList() ?: return@registerBukkitListener
            taskUuids.forEach { uuid ->
                val pipeTask = pipeTaskMap[uuid] ?: return@forEach
                val context = pipeTask.scriptContext
                val variables = context?.rootFrame()?.variables()?.toMap() ?: emptyMap()
                val shouldBreak = try {
                    trigger.onCheck(pipeTask, event, variables)
                } catch (ex: Throwable) {
                    warning("PipeTrigger $triggerKey 检查 PipeTask $uuid 时发生异常: ${ex.message}")
                    ex.printStackTrace()
                    false
                }
                if (shouldBreak) {
                    context?.let { trigger.onStart(it, event, variables) }
                    pipeTask.broke().whenComplete { _, _ ->
                        runOnMainThread {
                            context?.let { trigger.onEnd(it, event, it.rootFrame().variables().toMap()) }
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
        if (!pipeTaskMap.remove(pipeTask.uuid, pipeTask)) return
        pipeTask.brokeTriggers.forEach { rawKey ->
            val triggerKey = PipeTriggerKey.normalize(rawKey)
            triggerTaskIndex.computeIfPresent(triggerKey) { _, taskUuids ->
                taskUuids.remove(pipeTask.uuid)
                taskUuids.takeUnless { it.isEmpty() }
            }
            removeListenerIfUnused(triggerKey)
        }
    }

    private fun removeListenerIfUnused(triggerKey: String) {
        onMainThread {
            synchronized(listenerLock) {
                if (!triggerTaskIndex[triggerKey].isNullOrEmpty()) return@synchronized
                val trigger = TriggerManager.pipeTriggersMap[triggerKey] ?: return@synchronized
                trigger.listener?.let(::unregisterListener)
                trigger.listener = null
            }
        }
    }

    private fun onMainThread(action: () -> Unit) {
        if (isPrimaryThread) {
            action()
        } else {
            submit { action() }
        }
    }
}
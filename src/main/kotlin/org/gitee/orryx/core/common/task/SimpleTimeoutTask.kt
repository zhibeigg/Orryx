package org.gitee.orryx.core.common.task

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

open class SimpleTimeoutTask(val tick: Long, open val closed: () -> Unit = EMPTY) {

    val future = CompletableFuture<Void>()

    private val completed = AtomicBoolean(false)
    private val taskRef = AtomicReference<PlatformExecutor.PlatformTask?>()

    val task: PlatformExecutor.PlatformTask?
        get() = taskRef.get()

    private fun attach(task: PlatformExecutor.PlatformTask) {
        if (!taskRef.compareAndSet(null, task) || completed.get()) {
            task.cancel()
        }
    }

    internal fun finish(running: Boolean): Boolean {
        if (!completed.compareAndSet(false, true)) return false
        cache.remove(this)
        taskRef.getAndSet(null)?.cancel()
        future.complete(null)
        if (running) closed()
        return true
    }

    companion object {

        internal val EMPTY: () -> Unit = {}

        private val cache = ConcurrentHashMap.newKeySet<SimpleTimeoutTask>()

        @Awake(LifeCycle.DISABLE)
        fun unregisterAll() {
            cache.toList().forEach { it.finish(true) }
        }

        fun cancel(simpleTask: SimpleTimeoutTask, running: Boolean = true) {
            simpleTask.finish(running)
        }

        fun SimpleTimeoutTask.register(): SimpleTimeoutTask {
            if (!cache.add(this)) return this
            val scheduled = submit(delay = tick.coerceAtLeast(0L)) {
                finish(true)
            }
            attach(scheduled)
            return this
        }

        fun createSimpleTask(tick: Long, closed: () -> Unit): SimpleTimeoutTask {
            return SimpleTimeoutTask(tick, closed).register()
        }
    }
}
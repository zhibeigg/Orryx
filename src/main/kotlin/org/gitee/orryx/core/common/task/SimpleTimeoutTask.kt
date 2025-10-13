package org.gitee.orryx.core.common.task

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.common.util.unsafeLazy
import java.util.concurrent.CompletableFuture

open class SimpleTimeoutTask(val tick: Long, open val closed: () -> Unit = EMPTY) {

    var future = CompletableFuture<Void>()

    lateinit var task: PlatformExecutor.PlatformTask

    companion object {

        internal val EMPTY by unsafeLazy { {} }

        private val cache = mutableListOf<SimpleTimeoutTask>()

        @Awake(LifeCycle.DISABLE)
        fun unregisterAll() {
            val iterator = cache.iterator()
            while (iterator.hasNext()) {
                shutdown(iterator.next())
                iterator.remove()
            }
        }

        fun cancel(simpleTask: SimpleTimeoutTask, running: Boolean = true) {
            cache -= simpleTask
            simpleTask.task.cancel()
            // 如果已经结束了
            if (simpleTask.future.isDone) return
            simpleTask.future.complete(null)
            if (running) simpleTask.closed()
        }

        private fun shutdown(simpleTask: SimpleTimeoutTask, running: Boolean = true) {
            simpleTask.task.cancel()
            // 如果已经结束了
            if (simpleTask.future.isDone) return
            simpleTask.future.complete(null)
            if (running) simpleTask.closed()
        }

        fun SimpleTimeoutTask.register(): SimpleTimeoutTask {
            cache += this
            task = submit(delay = tick) {
                cancel(this@register)
            }
            return this
        }

        fun createSimpleTask(tick: Long, closed: () -> Unit): SimpleTimeoutTask {
            return SimpleTimeoutTask(tick, closed).register()
        }
    }
}
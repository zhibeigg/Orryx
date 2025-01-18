package org.gitee.orryx.core.common.task

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor

open class SimpleTimeoutTask(val tick: Long, open val closed: () -> Unit = EMPTY) {

    var isClosed = false

    lateinit var task: PlatformExecutor.PlatformTask

    companion object {

        internal val EMPTY by lazy { {} }

        private val cache = mutableListOf<SimpleTimeoutTask>()

        @Awake(LifeCycle.DISABLE)
        fun unregisterAll() {
            cache.forEach {
                // 未执行任务
                cancel(it)
            }
        }

        fun cancel(simpleTask: SimpleTimeoutTask) {
            cache -= simpleTask
            simpleTask.task.cancel()
            // 如果已经结束了
            if (simpleTask.isClosed) return
            simpleTask.isClosed = true
            simpleTask.closed()
        }

        fun SimpleTimeoutTask.register(): SimpleTimeoutTask {
            cache += this
            task = submit(delay = tick) {
                cancel(this@register)
            }
            return this
        }

        fun createSimpleTask(tick: Long, closed: () -> Unit) {
            SimpleTimeoutTask(tick, closed).register()
        }

    }

}
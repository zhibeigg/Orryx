package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.ensureSync
import org.gitee.orryx.utils.scriptParser
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionFuture
import taboolib.module.kether.long
import taboolib.module.kether.run

object Actions {

    @KetherParser(["wait", "delay", "sleep"], namespace = ORRYX_NAMESPACE)
    private fun actionWait() = scriptParser(
        Action.new("普通语句", "延迟delay", "wait/delay/sleep")
            .description("延迟多少Tick")
            .addEntry("tick", Type.LONG)
    ) {
        val ticks = it.nextParsedAction()
        actionFuture { f ->
            run(ticks).long { ticks ->
                val task = submit(delay = ticks, async = !isPrimaryThread) {
                    f.complete(null)
                }
                addOrryxCloseable(f) { task.cancel() }
            }
        }
    }

    @KetherParser(["sync"], namespace = ORRYX_NAMESPACE)
    private fun actionSync() = scriptParser(
        Action.new("普通语句", "同步Sync", "sync")
            .description("将语句在主线程运行并等待返回")
            .addEntry("actions", Type.ANY)
    ) {
        val action = it.nextParsedAction()
        actionFuture { future ->
            ensureSync { run(action).thenAccept { value -> future.complete(value) } }
        }
    }

    @KetherParser(["contains"], namespace = ORRYX_NAMESPACE)
    private fun actionContains() = scriptParser(
        Action.new("普通语句", "是否包含", "contains")
            .description("Iterable或者String是否包含value")
            .addEntry("Iterable或者String", Type.ANY)
            .addEntry("value", Type.ANY)
            .result("是否包含", Type.BOOLEAN)
    ) {
        val check = it.nextParsedAction()
        val value = it.nextParsedAction()
        actionFuture { future ->
            run(check).thenAccept { check ->
                run(value).thenAccept { value ->
                    future.complete(
                        when (check) {
                            is Iterable<*> -> check.contains(value)
                            is String -> check.contains(value.toString())
                            else -> false
                        }
                    )
                }
            }
        }
    }
}
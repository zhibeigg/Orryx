package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.common.util.sync
import taboolib.module.kether.*

object Actions {

    @KetherParser(["wait", "delay", "sleep"], namespace = ORRYX_NAMESPACE)
    private fun actionWait() = scriptParser(
        arrayOf(
            Action.new("普通语句", "延迟delay", "wait/delay/sleep")
                .description("延迟多少Tick")
                .addEntry("tick", Type.LONG)
        )
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
        arrayOf(
            Action.new("普通语句", "同步Sync", "sync")
                .description("将语句在主线程运行并等待返回")
                .addEntry("actions", Type.ANY)
        )
    ) {
        val actions = it.nextParsedAction()
        actionTake {
            sync {
                run(actions)
            }
        }
    }

}
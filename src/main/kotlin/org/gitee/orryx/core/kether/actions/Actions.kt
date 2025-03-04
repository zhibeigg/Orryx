package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.addOrryxCloseable
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import taboolib.common.platform.function.isPrimaryThread
import taboolib.common.platform.function.submit
import taboolib.module.kether.*

object Actions {

    @KetherParser(["wait", "delay", "sleep"], namespace = ORRYX_NAMESPACE)
    private fun actionWait() = scriptParser {
        val ticks = it.nextParsedAction()
        actionFuture { f ->
            run(ticks).long { ticks ->
                val task = submit(delay = ticks, async = !isPrimaryThread) {
                    f.complete(null)
                }
                addOrryxCloseable(f) { task.cancel() }
                addClosable(AutoCloseable { task.cancel() })
            }
        }
    }

}
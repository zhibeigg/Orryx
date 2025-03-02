package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import taboolib.module.kether.KetherParser
import taboolib.module.kether.ScriptAction
import taboolib.module.kether.actionFuture
import taboolib.module.kether.switch

object RandomActions {

    @KetherParser(["uuid"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun uuid() = scriptParser(
        arrayOf(
            Action.new("UUID唯一标识符", "随机生成UUID", "uuid", true)
                .addEntry("随机生成标识符", Type.SYMBOL, false, head = "random")
                .description("随机生成一个唯一UUID")
                .result("UUID", Type.STRING)
        )
    ) {
        it.switch {
            case("random") { randomUUID() }
            other { randomUUID() }
        }
    }

    private fun randomUUID(): ScriptAction<Any?> {
        return actionFuture { future ->
            future.complete(java.util.UUID.randomUUID().toString())
        }
    }

}
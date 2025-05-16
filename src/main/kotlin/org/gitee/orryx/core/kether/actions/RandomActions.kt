package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.combinationParser
import org.gitee.orryx.utils.scriptParser
import taboolib.module.kether.*

object RandomActions {

    @KetherParser(["uuid"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun uuid() = scriptParser(
        Action.new("UUID唯一标识符", "随机生成UUID", "uuid", true)
            .addEntry("随机生成标识符", Type.SYMBOL, false, head = "random")
            .description("随机生成一个唯一UUID")
            .result("UUID", Type.STRING)
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

    @KetherParser(["randomAction"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun randomAction() = combinationParser(
        Action.new("普通语句", "随机运行一段Action", "randomAction", true)
            .addEntry("语句列表[ { tell me }, { tell her } ]", Type.ITERABLE, false)
            .description("随机运行一段Action")
            .result("运行的Action返回值", Type.ANY)
    ) {
        it.group(
            actionList()
        ).apply(it) { array ->
            future {
                run(array.random())
            }
        }
    }
}
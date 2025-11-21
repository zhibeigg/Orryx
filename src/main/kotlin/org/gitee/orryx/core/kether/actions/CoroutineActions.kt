package org.gitee.orryx.core.kether.actions

import org.gitee.orryx.module.wiki.Action
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.scriptParser
import taboolib.common.platform.function.isPrimaryThread
import taboolib.module.kether.KetherParser
import taboolib.module.kether.actionNow

object CoroutineActions {

    @KetherParser(["inMain"], namespace = ORRYX_NAMESPACE, shared = true)
    private fun actionInMain() = scriptParser(
        Action.new("Coroutine协程", "检测线程", "inMain", true)
            .description("检测当前位置是否在主线程运行")
            .result("是否在主线程", Type.BOOLEAN)
    ) {
        actionNow { isPrimaryThread }
    }
}
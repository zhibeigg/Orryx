package org.gitee.orryx.core.kether.actions.station

import org.bukkit.event.Cancellable
import org.gitee.orryx.core.kether.ScriptManager.scriptParser
import org.gitee.orryx.core.kether.parameter.StationParameter
import org.gitee.orryx.core.wiki.Action
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.ORRYX_NAMESPACE
import org.gitee.orryx.utils.getParameter
import taboolib.module.kether.*

object EventActions {

    @KetherParser(["event"], namespace = ORRYX_NAMESPACE)
    private fun event() = scriptParser(
        arrayOf(
            Action.new("Station专属语句", "设置事件是否取消", "event")
                .description("设置当前Station监听到的事件是否取消")
                .addEntry("取消标识符", Type.SYMBOL, false, head = "cancelled")
                .addEntry("是否取消", Type.BOOLEAN, false)
        )
    ) {
        it.switch {
            case("cancelled") {
                val cancelled = nextParsedAction()
                actionNow {
                    run(cancelled).bool { cancelled ->
                        val parameter = script().getParameter()
                        if (parameter is StationParameter) {
                            if (parameter.event is Cancellable) {
                                parameter.event.isCancelled = cancelled
                            }
                        }
                    }
                }
            }
        }
    }


}
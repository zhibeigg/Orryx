package org.gitee.orryx.core.station.triggers

import org.gitee.orryx.core.station.WikiTrigger
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import taboolib.common.platform.event.ProxyListener
import taboolib.module.kether.ScriptContext

abstract class AbstractPipeEventTrigger<E>: IPipeTrigger<E>, WikiTrigger {

    override var listener: ProxyListener? = null

    override fun onEnd(context: ScriptContext, event: E, map: Map<String, Any?>) {
    }

}
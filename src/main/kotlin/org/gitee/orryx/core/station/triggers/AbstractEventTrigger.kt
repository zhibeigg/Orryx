package org.gitee.orryx.core.station.triggers

import org.gitee.orryx.core.station.WikiTrigger
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.stations.IStationTrigger
import taboolib.common.platform.event.ProxyListener
import taboolib.module.kether.ScriptContext

abstract class AbstractEventTrigger<E>: IStationTrigger<E>, IPipeTrigger<E>, WikiTrigger {

    override var listener: ProxyListener? = null

    override val specialKeys: Array<String> = emptyArray()

    override fun onRegister(station: IStation, map: Map<String, Any?>) {
    }

    override fun onCheck(station: IStation, event: E, map: Map<String, Any?>): Boolean {
        return station.event.equals(this.event, ignoreCase = true)
    }

    override fun onEnd(context: ScriptContext, event: E, map: Map<String, Any?>) {
    }
}
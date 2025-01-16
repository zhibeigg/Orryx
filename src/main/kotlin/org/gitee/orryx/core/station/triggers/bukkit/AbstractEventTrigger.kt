package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEvent
import org.gitee.orryx.core.station.pipe.IPipeTrigger
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.stations.IStationTrigger
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

abstract class AbstractEventTrigger<E : PlayerEvent>: IStationTrigger<E>, IPipeTrigger<E> {

    override var listener: ProxyListener? = null

    override val specialKeys: Array<String>
        get() = emptyArray()

    override fun onCheck(station: IStation, event: E, map: Map<String, Any?>): Boolean {
        return station.event.uppercase() == this.event.uppercase()
    }

    override fun onJoin(event: E, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onEnd(context: ScriptContext, event: E, map: Map<String, Any?>) {
    }

}
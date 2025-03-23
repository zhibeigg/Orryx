package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEvent
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

abstract class AbstractPlayerEventTrigger<E : PlayerEvent>: AbstractEventTrigger<E>() {

    override fun onJoin(event: E, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

}
package org.gitee.orryx.core.station.triggers

import org.bukkit.event.player.PlayerEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.module.wiki.Trigger
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

abstract class AbstractPropertyPlayerEventTrigger<E : PlayerEvent>(override val event: String): AbstractPropertyEventTrigger<E>(event) {

    override fun onJoin(event: E, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: E, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }
}
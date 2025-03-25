package org.gitee.orryx.core.station.triggers.bukkit

import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.platform.event.PlayerJumpEvent

object PlayerJumpTrigger: AbstractEventTrigger<PlayerJumpEvent>() {

    override val event: String = "Player Jump"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .description("玩家跳跃")

    override val clazz
        get() = PlayerJumpEvent::class.java

    override fun onJoin(event: PlayerJumpEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerJumpEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

}
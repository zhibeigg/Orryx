package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerKickEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerKickTrigger: AbstractPlayerEventTrigger<PlayerKickEvent>() {

    override val event: String = "Player Kick"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "leaveMessage", "退出信息")
            .addParm(Type.STRING, "reason", "原因")
            .description("玩家被踢出服务器时触发")

    override val clazz
        get() = PlayerKickEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerKickEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["reason"] = event.reason
        context["leaveMessage"] = event.leaveMessage
    }

}
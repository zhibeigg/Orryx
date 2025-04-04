package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerJoinEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerJoinTrigger: AbstractPlayerEventTrigger<PlayerJoinEvent>() {

    override val event: String = "Player Join"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "joinMessage", "进入信息")
            .description("玩家进入服务器时触发")

    override val clazz
        get() = PlayerJoinEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerJoinEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["joinMessage"] = event.joinMessage
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerMoveEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerMoveTrigger: AbstractPlayerEventTrigger<PlayerMoveEvent>() {

    override val event: String = "Player Move"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "to", "到达的位置")
            .addParm(Type.TARGET, "from", "来的位置")
            .description("玩家移动触发（高频）")

    override val clazz
        get() = PlayerMoveEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerMoveEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["to"] = event.to?.let { LocationTarget(it) }
        context["from"] = LocationTarget(event.from)
    }

}
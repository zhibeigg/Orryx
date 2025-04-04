package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChangedWorldEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerChangedWorldTrigger: AbstractPlayerEventTrigger<PlayerChangedWorldEvent>() {

    override val event: String = "Player Changed World"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "from", "从哪个世界来")
            .addParm(Type.STRING, "to", "到哪个世界去")
            .description("当玩家切换到另一个世界时触发")

    override val clazz
        get() = PlayerChangedWorldEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerChangedWorldEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["from"] = event.from.name
        context["to"] = event.player.world.name
    }

}
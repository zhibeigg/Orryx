package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerPickupArrowEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerPickupArrowTrigger: AbstractPlayerEventTrigger<PlayerPickupArrowEvent>() {

    override val event: String = "Player Pickup Arrow"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "arrow", "箭矢实体")
            .description("当玩家从地上捡起箭时触发")

    override val clazz
        get() = PlayerPickupArrowEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerPickupArrowEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["arrow"] = AbstractBukkitEntity(event.arrow)
    }

}
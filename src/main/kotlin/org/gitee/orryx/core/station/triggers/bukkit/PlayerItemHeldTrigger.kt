package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemHeldEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerItemHeldTrigger: AbstractPlayerEventTrigger<PlayerItemHeldEvent>() {

    override val event: String = "Player Item Held"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newSlot", "新格子")
            .addParm(Type.INT, "previousSlot", "旧格子")
            .description("玩家改变手持某物品时触发")

    override val clazz
        get() = PlayerItemHeldEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerItemHeldEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["newSlot"] = event.newSlot
        context["previousSlot"] = event.previousSlot
        context["newItemStack"] = event.player.inventory.getItem(event.newSlot)
        context["previousItemStack"] = event.player.inventory.getItem(event.previousSlot)
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerDropItemEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerDropItemTrigger: AbstractPlayerEventTrigger<PlayerDropItemEvent>() {

    override val event: String = "Player Drop Item"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "itemDrop", "获得此玩家丢出的物品实体")
            .addParm(Type.ITEM_STACK, "itemStackDrop", "获得此玩家丢出的物品")
            .description("玩家丢出物品时触发")

    override val clazz
        get() = PlayerDropItemEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerDropItemEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["itemDrop"] = AbstractBukkitEntity(event.itemDrop)
        context["itemStackDrop"] = event.itemDrop.itemStack
    }
}
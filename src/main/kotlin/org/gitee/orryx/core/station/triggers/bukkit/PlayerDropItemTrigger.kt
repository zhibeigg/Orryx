package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerDropItemEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerDropItemTrigger: AbstractEventTrigger<PlayerDropItemEvent>() {

    override val event: String = "Player Drop Item"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "itemDrop", "获得此玩家丢出的物品实体")
            .addParm(Type.ITEM_STACK, "itemStackDrop", "获得此玩家丢出的物品")
            .description("玩家丢出物品时触发")

    override val clazz
        get() = PlayerDropItemEvent::class.java

    override fun onJoin(event: PlayerDropItemEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerDropItemEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerDropItemEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["itemDrop"] = AbstractBukkitEntity(event.itemDrop)
        context["itemStackDrop"] = event.itemDrop.itemStack
    }

}
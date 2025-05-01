package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerDropItemEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult

object PlayerDropItemTrigger: AbstractPropertyPlayerEventTrigger<PlayerDropItemEvent>("Player Drop Item") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "itemDrop", "获得此玩家丢出的物品实体")
            .addParm(Type.ITEM_STACK, "itemStackDrop", "获得此玩家丢出的物品")
            .description("玩家丢出物品时触发")

    override val clazz
        get() = PlayerDropItemEvent::class.java

    override fun read(instance: PlayerDropItemEvent, key: String): OpenResult {
        return when(key) {
            "itemDrop" -> OpenResult.successful(instance.itemDrop.abstract())
            "itemStackDrop" -> OpenResult.successful(instance.itemDrop.itemStack)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerDropItemEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
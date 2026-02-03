package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object PlayerPickupItemTrigger: AbstractPropertyEventTrigger<EntityPickupItemEvent>("Player Pickup Item") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "item", "掉落物实体")
            .addParm(Type.ITEM_STACK, "itemStack", "掉落物")
            .addParm(Type.INT, "remaining", "获得地面剩余掉落物品数量")
            .description("当玩家从地上捡起掉落物时触发")

    override val clazz
        get() = EntityPickupItemEvent::class.java

    override fun onCheck(station: IStation, event: EntityPickupItemEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.entity is Player
    }

    override fun onJoin(event: EntityPickupItemEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.entity)
    }

    override fun onCheck(pipeTask: IPipeTask, event: EntityPickupItemEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.entity
    }

    override fun read(instance: EntityPickupItemEvent, key: String): OpenResult {
        return when(key) {
            "item" -> OpenResult.successful(instance.item.abstract())
            "itemStack" -> OpenResult.successful(instance.item.itemStack)
            "remaining" -> OpenResult.successful(instance.remaining)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: EntityPickupItemEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
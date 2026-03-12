package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
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

object EntityShootBowTrigger: AbstractPropertyEventTrigger<EntityShootBowEvent>("Entity Shoot Bow") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "bow", "使用的弓")
            .addParm(Type.TARGET, "projectile", "发射的投射物")
            .addParm(Type.FLOAT, "force", "弓的拉力")
            .addParm(Type.STRING, "hand", "使用的手")
            .description("当实体射箭时触发（仅玩家）")

    override val clazz
        get() = EntityShootBowEvent::class.java

    override fun onCheck(station: IStation, event: EntityShootBowEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.entity is Player
    }

    override fun onJoin(event: EntityShootBowEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.entity as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: EntityShootBowEvent, map: Map<String, Any?>): Boolean {
        return event.entity is Player && pipeTask.scriptContext?.sender?.origin == event.entity
    }

    override fun read(instance: EntityShootBowEvent, key: String): OpenResult {
        return when(key) {
            "bow" -> OpenResult.successful(instance.bow)
            "projectile" -> OpenResult.successful(instance.projectile.abstract())
            "force" -> OpenResult.successful(instance.force)
            "hand" -> OpenResult.successful(instance.hand?.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: EntityShootBowEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

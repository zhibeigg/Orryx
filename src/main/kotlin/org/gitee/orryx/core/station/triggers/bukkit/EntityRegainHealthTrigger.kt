package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cdouble

object EntityRegainHealthTrigger: AbstractPropertyEventTrigger<EntityRegainHealthEvent>("Entity Regain Health") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.DOUBLE, "amount", "恢复的生命值")
            .addParm(Type.STRING, "reason", "恢复原因")
            .description("当玩家恢复生命值时触发")

    override val clazz
        get() = EntityRegainHealthEvent::class.java

    override fun onCheck(station: IStation, event: EntityRegainHealthEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.entity is Player
    }

    override fun onJoin(event: EntityRegainHealthEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.entity as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: EntityRegainHealthEvent, map: Map<String, Any?>): Boolean {
        return event.entity is Player && pipeTask.scriptContext?.sender?.origin == event.entity
    }

    override fun read(instance: EntityRegainHealthEvent, key: String): OpenResult {
        return when(key) {
            "amount" -> OpenResult.successful(instance.amount)
            "reason" -> OpenResult.successful(instance.regainReason.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: EntityRegainHealthEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "amount" -> {
                instance.amount = value.cdouble
                OpenResult.successful(instance.amount)
            }
            else -> OpenResult.failed()
        }
    }
}

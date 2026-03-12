package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileLaunchEvent
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

object ProjectileLaunchTrigger: AbstractPropertyEventTrigger<ProjectileLaunchEvent>("Projectile Launch") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "projectile", "投射物实体")
            .addParm(Type.STRING, "entityType", "投射物类型")
            .description("当玩家发射投射物时触发")

    override val clazz
        get() = ProjectileLaunchEvent::class.java

    override fun onCheck(station: IStation, event: ProjectileLaunchEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.entity.shooter is Player
    }

    override fun onJoin(event: ProjectileLaunchEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.entity.shooter as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: ProjectileLaunchEvent, map: Map<String, Any?>): Boolean {
        return event.entity.shooter is Player && pipeTask.scriptContext?.sender?.origin == event.entity.shooter
    }

    override fun read(instance: ProjectileLaunchEvent, key: String): OpenResult {
        return when(key) {
            "projectile" -> OpenResult.successful(instance.entity.abstract())
            "entityType" -> OpenResult.successful(instance.entityType.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ProjectileLaunchEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

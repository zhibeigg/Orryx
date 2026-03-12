package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object ProjectileHitTrigger: AbstractPropertyEventTrigger<ProjectileHitEvent>("Projectile Hit") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "projectile", "投射物实体")
            .addParm(Type.TARGET, "hitEntity", "命中的实体")
            .addParm(Type.TARGET, "hitBlock", "命中的方块位置")
            .description("当投射物命中时触发（仅玩家发射）")

    override val clazz
        get() = ProjectileHitEvent::class.java

    override fun onCheck(station: IStation, event: ProjectileHitEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.entity.shooter is Player
    }

    override fun onJoin(event: ProjectileHitEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.entity.shooter as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: ProjectileHitEvent, map: Map<String, Any?>): Boolean {
        return event.entity.shooter is Player && pipeTask.scriptContext?.sender?.origin == event.entity.shooter
    }

    override fun read(instance: ProjectileHitEvent, key: String): OpenResult {
        return when(key) {
            "projectile" -> OpenResult.successful(instance.entity.abstract())
            "hitEntity" -> OpenResult.successful(instance.hitEntity?.abstract())
            "hitBlock" -> OpenResult.successful(instance.hitBlock?.location?.toTarget())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ProjectileHitEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

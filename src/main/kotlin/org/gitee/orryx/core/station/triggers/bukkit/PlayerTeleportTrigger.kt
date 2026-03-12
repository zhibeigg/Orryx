package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerTeleportEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerTeleportTrigger: AbstractPropertyPlayerEventTrigger<PlayerTeleportEvent>("Player Teleport") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "to", "目标位置")
            .addParm(Type.TARGET, "from", "来源位置")
            .addParm(Type.STRING, "cause", "传送原因")
            .description("当玩家传送时触发")

    override val clazz
        get() = PlayerTeleportEvent::class.java

    override fun read(instance: PlayerTeleportEvent, key: String): OpenResult {
        return when(key) {
            "to" -> OpenResult.successful(instance.to?.toTarget())
            "from" -> OpenResult.successful(instance.from.toTarget())
            "cause" -> OpenResult.successful(instance.cause.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerTeleportEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "to" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.setTo(it) }
                OpenResult.successful()
            }
            "from" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.from = it }
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

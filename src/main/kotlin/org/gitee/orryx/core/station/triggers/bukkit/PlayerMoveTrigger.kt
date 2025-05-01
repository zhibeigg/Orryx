package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerLevelChangeEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object PlayerMoveTrigger: AbstractPropertyPlayerEventTrigger<PlayerMoveEvent>("Player Move") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "to", "到达的位置")
            .addParm(Type.TARGET, "from", "来的位置")
            .description("玩家移动触发（高频）")

    override val clazz
        get() = PlayerMoveEvent::class.java

    override fun read(instance: PlayerMoveEvent, key: String): OpenResult {
        return when(key) {
            "to" -> OpenResult.successful(instance.to?.toTarget())
            "from" -> OpenResult.successful(instance.from.toTarget())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerMoveEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "from" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.from = it }
                OpenResult.successful()
            }
            "to" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.setTo(it) }
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
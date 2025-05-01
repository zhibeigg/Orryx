package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object PlayerPickupArrowTrigger: AbstractPropertyPlayerEventTrigger<PlayerPickupArrowEvent>("Player Pickup Arrow") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "arrow", "箭矢实体")
            .addParm(Type.TARGET, "remaining", "剩余掉落箭矢数量")
            .description("当玩家从地上捡起箭时触发")

    override val clazz
        get() = PlayerPickupArrowEvent::class.java

    override fun read(instance: PlayerPickupArrowEvent, key: String): OpenResult {
        return when(key) {
            "arrow" -> OpenResult.successful(instance.arrow.abstract())
            "remaining" -> OpenResult.successful(instance.remaining)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerPickupArrowEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
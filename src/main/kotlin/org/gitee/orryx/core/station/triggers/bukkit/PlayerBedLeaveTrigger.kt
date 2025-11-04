package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBedLeaveEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerBedLeaveTrigger: AbstractPropertyPlayerEventTrigger<PlayerBedLeaveEvent>("Player Bed Leave") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "bed", "床的位置")
            .addParm(Type.BOOLEAN, "shouldSetSpawn", "是否需要设置出生点")
            .description("玩家离开床时触发")

    override val clazz: java
        get() = PlayerBedLeaveEvent::class.java

    override fun read(instance: PlayerBedLeaveEvent, key: String): OpenResult {
        return when(key) {
            "bed" -> OpenResult.successful(instance.bed.location.toTarget())
            "shouldSetSpawn" -> OpenResult.successful(instance.shouldSetSpawnLocation())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerBedLeaveEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerLevelChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerLevelChangeTrigger: AbstractPropertyPlayerEventTrigger<PlayerLevelChangeEvent>("Player Level Change") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newLevel", "新等级")
            .addParm(Type.INT, "oldLevel", "老等级")
            .description("玩家等级改变时触发")

    override val clazz: java
        get() = PlayerLevelChangeEvent::class.java

    override fun read(instance: PlayerLevelChangeEvent, key: String): OpenResult {
        return when(key) {
            "newLevel" -> OpenResult.successful(instance.newLevel)
            "oldLevel" -> OpenResult.successful(instance.oldLevel)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerLevelChangeEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
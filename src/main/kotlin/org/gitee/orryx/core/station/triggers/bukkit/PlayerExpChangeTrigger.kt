package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerExpChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint

object PlayerExpChangeTrigger: AbstractPropertyPlayerEventTrigger<PlayerExpChangeEvent>("Player Exp Change") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "amount", "经验")
            .description("当玩家经验值发生变化时触发")

    override val clazz: java
        get() = PlayerExpChangeEvent::class.java

    override fun read(instance: PlayerExpChangeEvent, key: String): OpenResult {
        return when(key) {
            "amount" -> OpenResult.successful(instance.amount)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerExpChangeEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "amount" -> {
                instance.amount = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
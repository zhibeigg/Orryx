package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerExpCooldownChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint

object PlayerExpCooldownChangeTrigger: AbstractPropertyPlayerEventTrigger<PlayerExpCooldownChangeEvent>("Player Exp Cooldown Change") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newCooldown", "新的冷却时间")
            .addParm(Type.INT, "reason", "原因：PICKUP_ORB/PLUGIN")
            .description("当玩家经验冷却时间发生变化时触发")

    override val clazz: java
        get() = PlayerExpCooldownChangeEvent::class.java

    override fun read(instance: PlayerExpCooldownChangeEvent, key: String): OpenResult {
        return when(key) {
            "newCooldown" -> OpenResult.successful(instance.newCooldown)
            "reason" -> OpenResult.successful(instance.reason)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerExpCooldownChangeEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "newCooldown" -> {
                instance.newCooldown = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
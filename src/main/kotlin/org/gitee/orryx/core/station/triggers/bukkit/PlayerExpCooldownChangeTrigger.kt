package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerExpCooldownChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerExpCooldownChangeTrigger: AbstractPlayerEventTrigger<PlayerExpCooldownChangeEvent>() {

    override val event: String = "Player Exp Cooldown Change"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newCooldown", "新的冷却时间")
            .addParm(Type.INT, "reason", "原因：PICKUP_ORB/PLUGIN")
            .description("当玩家经验冷却时间发生变化时触发")

    override val clazz
        get() = PlayerExpCooldownChangeEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerExpCooldownChangeEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["newCooldown"] = event.newCooldown
        context["reason"] = event.reason.name
    }

}
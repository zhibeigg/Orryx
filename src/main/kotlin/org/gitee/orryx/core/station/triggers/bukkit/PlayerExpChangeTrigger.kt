package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerExpChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerExpChangeTrigger: AbstractPlayerEventTrigger<PlayerExpChangeEvent>() {

    override val event: String = "Player Exp Change"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "amount", "经验")
            .description("当玩家经验值发生变化时触发")

    override val clazz
        get() = PlayerExpChangeEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerExpChangeEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["amount"] = event.amount
    }

}
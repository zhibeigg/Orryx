package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerAnimationEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerAnimationTrigger: AbstractPlayerEventTrigger<PlayerAnimationEvent>() {

    override val event: String = "Player Animation"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "animation", "动作名")
            .description("玩家动作")

    override val clazz
        get() = PlayerAnimationEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerAnimationEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["animation"] = event.animationType.name
    }

}
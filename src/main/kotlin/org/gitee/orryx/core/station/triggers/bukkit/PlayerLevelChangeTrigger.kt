package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerLevelChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerLevelChangeTrigger: AbstractPlayerEventTrigger<PlayerLevelChangeEvent>() {

    override val event: String = "Player Level Change"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newLevel", "新等级")
            .addParm(Type.INT, "oldLevel", "老等级")
            .description("玩家等级改变时触发")

    override val clazz
        get() = PlayerLevelChangeEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerLevelChangeEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["newLevel"] = event.newLevel
        context["oldLevel"] = event.oldLevel
    }

}
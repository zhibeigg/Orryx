package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBedLeaveEvent
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerBedLeaveTrigger: AbstractPlayerEventTrigger<PlayerBedLeaveEvent>() {

    override val event: String = "Player Bed Leave"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "bed", "床的位置")
            .addParm(Type.BOOLEAN, "shouldSetSpawn", "是否需要设置出生点")
            .description("玩家离开床时触发")

    override val clazz
        get() = PlayerBedLeaveEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerBedLeaveEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["bed"] = LocationTarget(event.bed.location)
        context["shouldSetSpawn"] = event.shouldSetSpawnLocation()
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerHideEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerHideEntityTrigger: AbstractPlayerEventTrigger<PlayerHideEntityEvent>() {

    override val event: String = "Player Hide Entity"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "entity", "实体")
            .description("当可见实体对玩家隐藏时触发")

    override val clazz
        get() = PlayerHideEntityEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerHideEntityEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["entity"] = AbstractBukkitEntity(event.entity)
    }

}
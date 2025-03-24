package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.module.kether.ScriptContext

object PlayerInteractAtEntityTrigger: AbstractPlayerEventTrigger<PlayerInteractAtEntityEvent>() {

    override val event: String = "Player Interact At Entity"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.VECTOR, "clickedPosition", "点击的方向向量")
            .addParm(Type.TARGET, "rightClicked", "被点击的实体")
            .description("当玩家在实体上点击某实体上的某位置时触发")

    override val clazz
        get() = PlayerInteractAtEntityEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerInteractAtEntityEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["clickedPosition"] = event.clickedPosition.abstract()
        context["rightClicked"] = AbstractBukkitEntity(event.rightClicked)
    }

}
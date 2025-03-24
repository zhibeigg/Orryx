package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerInteractEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerInteractEntityTrigger: AbstractPlayerEventTrigger<PlayerInteractEntityEvent>() {

    override val event: String = "Player Interact Entity"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "rightClicked", "被点击的实体")
            .description("当玩家点击一个实体时调用")

    override val clazz
        get() = PlayerInteractEntityEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerInteractEntityEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["rightClicked"] = AbstractBukkitEntity(event.rightClicked)
    }

}
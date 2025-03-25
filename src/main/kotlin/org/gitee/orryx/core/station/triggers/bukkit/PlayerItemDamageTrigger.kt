package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemDamageEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerItemDamageTrigger: AbstractPlayerEventTrigger<PlayerItemDamageEvent>() {

    override val event: String = "Player Item Damage"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "损伤的物品")
            .addParm(Type.INT, "damage", "损伤的耐久")
            .description("当玩家使用的物品因使用而受到耐久性损坏时触发")

    override val clazz
        get() = PlayerItemDamageEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerItemDamageEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["item"] = event.item
        context["damage"] = event.damage
    }

}
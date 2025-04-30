package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemMendEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerItemMendTrigger: AbstractPlayerEventTrigger<PlayerItemMendEvent>() {

    override val event: String = "Player Item Mend"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "物品")
            .addParm(Type.STRING, "slot", "装备位置")
            .addParm(Type.INT, "repairAmount", "修复值")
            .addParm(Type.INT, "experience", "经验")
            .description("当玩家通过装备上的经验修补附魔修复装备耐久时触发")

    override val clazz
        get() = PlayerItemMendEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerItemMendEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["item"] = event.item
        context["slot"] = event.slot.name
        context["repairAmount"] = event.repairAmount
        context["experience"] = event.experienceOrb.experience
    }
}
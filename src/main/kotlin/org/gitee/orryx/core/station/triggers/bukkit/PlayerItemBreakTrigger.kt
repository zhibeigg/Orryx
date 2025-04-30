package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemBreakEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerItemBreakTrigger: AbstractPlayerEventTrigger<PlayerItemBreakEvent>() {

    override val event: String = "Player Item Break"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "brokenItem", "损坏的物品")
            .description("某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)")

    override val clazz
        get() = PlayerItemBreakEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerItemBreakEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["brokenItem"] = event.brokenItem
    }
}
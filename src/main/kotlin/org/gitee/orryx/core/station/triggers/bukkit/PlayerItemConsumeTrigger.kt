package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemConsumeEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerItemConsumeTrigger: AbstractPlayerEventTrigger<PlayerItemConsumeEvent>() {

    override val event: String = "Player Item Consume"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "消耗完的物品")
            .addParm(Type.STRING, "hand", "获取此事件中使用的手：OFF_HAND/HAND")
            .description("某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)")

    override val clazz
        get() = PlayerItemConsumeEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerItemConsumeEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["item"] = event.item
        context["hand"] = event.hand
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerInteractEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.module.kether.ScriptContext

object PlayerInteractTrigger: AbstractPlayerEventTrigger<PlayerInteractEvent>() {

    override val event: String = "Player Interact"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.VECTOR, "clickedPosition", "点击的方向向量")
            .addParm(Type.ITEM_STACK, "item", "点击物品")
            .addParm(Type.STRING, "action", "点击动作类型：LEFT_CLICK_AIR/LEFT_CLICK_BLOCK/PHYSICAL/RIGHT_CLICK_AIR/RIGHT_CLICK_BLOCK")
            .addParm(Type.TARGET, "clickedBlock", "被点击的方块位置")
            .description("当玩家对一个对象或空气进行交互时触发")

    override val clazz
        get() = PlayerInteractEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerInteractEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["clickedPosition"] = event.clickedPosition?.abstract()
        context["item"] = event.item
        context["action"] = event.action.name
        context["clickedBlock"] = event.clickedBlock?.location?.let { LocationTarget(it) }
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerHarvestBlockTrigger: AbstractPlayerEventTrigger<PlayerHarvestBlockEvent>() {

    override val event: String = "Player Harvest Block"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "harvestedBlock", "获取被收获的方块位置")
            .addParm(Type.ITERABLE, "itemsHarvested", "获取从此方块收获的物品列表")
            .addParm(Type.STRING, "hand", "获取用于收获方块的手：OFF_HAND/HAND")
            .description("当玩家收获作物方块时触发")

    override val clazz
        get() = PlayerHarvestBlockEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerHarvestBlockEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["harvestedBlock"] = LocationTarget(event.harvestedBlock.location)
        context["itemsHarvested"] = event.itemsHarvested
        context["hand"] = event.hand.name
    }

}
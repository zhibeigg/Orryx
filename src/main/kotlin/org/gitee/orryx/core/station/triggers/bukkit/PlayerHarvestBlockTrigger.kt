package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerHarvestBlockTrigger: AbstractPropertyPlayerEventTrigger<PlayerHarvestBlockEvent>("Player Harvest Block") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "harvestedBlock", "获取被收获的方块位置")
            .addParm(Type.ITERABLE, "itemsHarvested", "获取从此方块收获的物品列表")
            .addParm(Type.STRING, "hand", "获取用于收获方块的手：OFF_HAND/HAND")
            .description("当玩家收获作物方块时触发")

    override val clazz
        get() = PlayerHarvestBlockEvent::class.java

    override fun read(instance: PlayerHarvestBlockEvent, key: String): OpenResult {
        return when(key) {
            "harvestedBlock" -> OpenResult.successful(instance.harvestedBlock.location.toTarget())
            "itemsHarvested" -> OpenResult.successful(instance.itemsHarvested)
            "hand" -> OpenResult.successful(instance.hand.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerHarvestBlockEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
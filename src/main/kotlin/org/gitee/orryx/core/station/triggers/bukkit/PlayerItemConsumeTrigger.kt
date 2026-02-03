package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerItemConsumeTrigger: AbstractPropertyPlayerEventTrigger<PlayerItemConsumeEvent>("Player Item Consume") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "消耗完的物品")
            .addParm(Type.STRING, "hand", "获取此事件中使用的手：OFF_HAND/HAND")
            .description("某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)")

    override val clazz
        get() = PlayerItemConsumeEvent::class.java

    override fun read(instance: PlayerItemConsumeEvent, key: String): OpenResult {
        return when(key) {
            "item" -> OpenResult.successful(instance.item)
            "hand" -> OpenResult.successful(instance.hand.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerItemConsumeEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "item" -> {
                instance.setItem(value as ItemStack?)
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
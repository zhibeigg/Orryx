package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerSwapHandItemsTrigger: AbstractPropertyPlayerEventTrigger<PlayerSwapHandItemsEvent>("Player Swap Hand Items") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "mainHandItem", "主手物品")
            .addParm(Type.ITEM_STACK, "offHandItem", "副手物品")
            .description("当玩家切换主副手物品时触发")

    override val clazz
        get() = PlayerSwapHandItemsEvent::class.java

    override fun read(instance: PlayerSwapHandItemsEvent, key: String): OpenResult {
        return when(key) {
            "mainHandItem" -> OpenResult.successful(instance.mainHandItem)
            "offHandItem" -> OpenResult.successful(instance.offHandItem)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerSwapHandItemsEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

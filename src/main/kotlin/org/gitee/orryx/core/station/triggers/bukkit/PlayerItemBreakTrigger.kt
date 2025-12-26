package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemBreakEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerItemBreakTrigger: AbstractPropertyPlayerEventTrigger<PlayerItemBreakEvent>("Player Item Break") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "brokenItem", "损坏的物品")
            .description("某玩家工具耐久消耗完毕时触发(比如铲子，打火石，铁制工具)")

    override val clazz: java
        get() = PlayerItemBreakEvent::class.java

    override fun read(instance: PlayerItemBreakEvent, key: String): OpenResult {
        return when(key) {
            "brokenItem" -> OpenResult.successful(instance.brokenItem)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerItemBreakEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
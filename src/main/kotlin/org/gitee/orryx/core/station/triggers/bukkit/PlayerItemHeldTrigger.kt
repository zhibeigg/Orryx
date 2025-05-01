package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint
import taboolib.module.kether.ScriptContext

object PlayerItemHeldTrigger: AbstractPropertyPlayerEventTrigger<PlayerItemHeldEvent>("Player Item Held") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.INT, "newSlot", "新格子")
            .addParm(Type.INT, "previousSlot", "旧格子")
            .addParm(Type.INT, "newItemStack", "新格子中物品")
            .addParm(Type.INT, "previousItemStack", "旧格子中物品")
            .description("玩家改变手持某物品时触发")

    override val clazz
        get() = PlayerItemHeldEvent::class.java

    override fun read(instance: PlayerItemHeldEvent, key: String): OpenResult {
        return when(key) {
            "newSlot" -> OpenResult.successful(instance.newSlot)
            "previousSlot" -> OpenResult.successful(instance.previousSlot)
            "newItemStack" -> OpenResult.successful(instance.player.inventory.getItem(instance.newSlot))
            "previousItemStack" -> OpenResult.successful(instance.player.inventory.getItem(instance.previousSlot))
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerItemHeldEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
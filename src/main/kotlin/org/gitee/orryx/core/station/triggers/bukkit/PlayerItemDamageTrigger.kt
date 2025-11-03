package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemDamageEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint

object PlayerItemDamageTrigger: AbstractPropertyPlayerEventTrigger<PlayerItemDamageEvent>("Player Item Damage") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "损伤的物品")
            .addParm(Type.INT, "damage", "损伤的耐久")
            .description("当玩家使用的物品因使用而受到耐久性损坏时触发")

    override val clazz: java
        get() = PlayerItemDamageEvent::class.java

    override fun read(instance: PlayerItemDamageEvent, key: String): OpenResult {
        return when(key) {
            "item" -> OpenResult.successful(instance.item)
            "damage" -> OpenResult.successful(instance.damage)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerItemDamageEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "damage" -> {
                instance.damage = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
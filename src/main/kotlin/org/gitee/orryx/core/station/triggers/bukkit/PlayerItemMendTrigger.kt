package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerItemMendEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cint

object PlayerItemMendTrigger: AbstractPropertyPlayerEventTrigger<PlayerItemMendEvent>("Player Item Mend") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "item", "物品")
            .addParm(Type.STRING, "slot", "装备位置")
            .addParm(Type.INT, "repairAmount", "修复值")
            .addParm(Type.INT, "experience", "经验")
            .description("当玩家通过装备上的经验修补附魔修复装备耐久时触发")

    override val clazz
        get() = PlayerItemMendEvent::class.java

    override fun read(instance: PlayerItemMendEvent, key: String): OpenResult {
        return when(key) {
            "item" -> OpenResult.successful(instance.item)
            "slot" -> OpenResult.successful(instance.slot.name)
            "repairAmount" -> OpenResult.successful(instance.repairAmount)
            "experience" -> OpenResult.successful(instance.experienceOrb.experience)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerItemMendEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "repairAmount" -> {
                instance.repairAmount = value.cint
                OpenResult.successful()
            }
            "experience" -> {
                instance.experienceOrb.experience = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerArmorStandManipulateTrigger: AbstractPropertyPlayerEventTrigger<PlayerArmorStandManipulateEvent>("Player ArmorStand Manipulate") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITEM_STACK, "armorStandItem", "盔甲架的物品")
            .addParm(Type.ITEM_STACK, "playerItem", "玩家的物品")
            .addParm(Type.STRING, "slot", "玩家的EquipmentSlot：BODY/CHEST/FEET/HAND/HEAD/LEGS/OFF_HAND")
            .addParm(Type.TARGET, "rightClicked", "玩家右键的实体")
            .addParm(Type.STRING, "hand", "玩家使用的手的EquipmentSlot：HAND/OFF_HAND")
            .description("当玩家与装甲架交互并且进行交换, 取回或放置物品时触发")

    override val clazz: java
        get() = PlayerArmorStandManipulateEvent::class.java

    override fun read(instance: PlayerArmorStandManipulateEvent, key: String): OpenResult {
        return when(key) {
            "armorStandItem" -> OpenResult.successful(instance.armorStandItem)
            "playerItem" -> OpenResult.successful(instance.playerItem)
            "slot" -> OpenResult.successful(instance.slot.name)
            "rightClicked" -> OpenResult.successful(instance.rightClicked)
            "hand" -> OpenResult.successful(instance.hand.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerArmorStandManipulateEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerInteractEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerInteractTrigger: AbstractPropertyPlayerEventTrigger<PlayerInteractEvent>("Player Interact") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.VECTOR, "clickedPosition", "点击的方向向量")
            .addParm(Type.ITEM_STACK, "item", "点击物品")
            .addParm(Type.STRING, "action", "点击动作类型：LEFT_CLICK_AIR/LEFT_CLICK_BLOCK/PHYSICAL/RIGHT_CLICK_AIR/RIGHT_CLICK_BLOCK")
            .addParm(Type.TARGET, "clickedBlock", "被点击的方块位置")
            .description("当玩家对一个对象或空气进行交互时触发")

    override val clazz: java
        get() = PlayerInteractEvent::class.java

    override fun read(instance: PlayerInteractEvent, key: String): OpenResult {
        return when(key) {
            "clickedPosition" -> OpenResult.successful(instance.clickedPosition?.abstract())
            "item" -> OpenResult.successful(instance.item)
            "action" -> OpenResult.successful(instance.action.name)
            "clickedBlock" -> OpenResult.successful(instance.clickedBlock?.location?.toTarget())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerInteractEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
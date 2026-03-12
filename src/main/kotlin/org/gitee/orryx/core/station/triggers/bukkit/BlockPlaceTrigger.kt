package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.block.BlockPlaceEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object BlockPlaceTrigger: AbstractPropertyEventTrigger<BlockPlaceEvent>("Block Place") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "block", "放置的方块位置")
            .addParm(Type.TARGET, "blockAgainst", "被放置方块依附的方块位置")
            .addParm(Type.STRING, "hand", "使用的手")
            .addParm(Type.ITEM_STACK, "itemInHand", "手中的物品")
            .description("当玩家放置方块时触发")

    override val clazz
        get() = BlockPlaceEvent::class.java

    override fun onJoin(event: BlockPlaceEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: BlockPlaceEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: BlockPlaceEvent, key: String): OpenResult {
        return when(key) {
            "block" -> OpenResult.successful(instance.block.location.toTarget())
            "blockAgainst" -> OpenResult.successful(instance.blockAgainst.location.toTarget())
            "hand" -> OpenResult.successful(instance.hand.name)
            "itemInHand" -> OpenResult.successful(instance.itemInHand)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: BlockPlaceEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

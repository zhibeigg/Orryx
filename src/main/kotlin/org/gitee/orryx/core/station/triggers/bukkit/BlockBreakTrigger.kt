package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.block.BlockBreakEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint

object BlockBreakTrigger: AbstractPropertyEventTrigger<BlockBreakEvent>("Block Break") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "block", "被破坏的方块位置")
            .addParm(Type.INT, "expToDrop", "掉落的经验值")
            .description("当玩家破坏方块时触发")

    override val clazz
        get() = BlockBreakEvent::class.java

    override fun onJoin(event: BlockBreakEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: BlockBreakEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: BlockBreakEvent, key: String): OpenResult {
        return when(key) {
            "block" -> OpenResult.successful(instance.block.location.toTarget())
            "expToDrop" -> OpenResult.successful(instance.expToDrop)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: BlockBreakEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "expToDrop" -> {
                instance.expToDrop = value.cint
                OpenResult.successful(instance.expToDrop)
            }
            else -> OpenResult.failed()
        }
    }
}

package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.PlayerSlotUpdateEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

@Plugin("DragonCore")
object DragonSlotTrigger: AbstractPropertyPlayerEventTrigger<PlayerSlotUpdateEvent>("Dragon Slot Update") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "identifier", "槽位名")
            .addParm(Type.ITERABLE, "item", "物品")
            .addSpecialKey(Type.STRING, "Identifier", "槽位名")
            .description("玩家龙核槽位更新")

    override val clazz: java
        get() = PlayerSlotUpdateEvent::class.java

    override val specialKeys: Array<String> = arrayOf("Identifier")

    override fun onCheck(station: IStation, event: PlayerSlotUpdateEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && (map["Identifier"] == null || event.identifier == map["Identifier"])
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerSlotUpdateEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player) && (map["Identifier"] == null || event.identifier == map["Identifier"])
    }

    override fun read(instance: PlayerSlotUpdateEvent, key: String): OpenResult {
        return when(key) {
            "identifier" -> OpenResult.successful(instance.identifier)
            "item" -> OpenResult.successful(instance.itemStack)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerSlotUpdateEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
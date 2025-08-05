package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object DragonPacketTrigger: AbstractPropertyPlayerEventTrigger<CustomPacketEvent>("Dragon Packet") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "identifier", "包名")
            .addParm(Type.ITERABLE, "data", "包数据")
            .addSpecialKey(Type.STRING, "Identifier", "包名")
            .description("玩家龙核发包")

    override val clazz
        get() = CustomPacketEvent::class.java

    override val specialKeys: Array<String> = arrayOf("Identifier")

    override fun onCheck(station: IStation, event: CustomPacketEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && (map["identifier"] == null || event.identifier == map["identifier"])
    }

    override fun onCheck(pipeTask: IPipeTask, event: CustomPacketEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player) && (map["identifier"] == null || event.identifier == map["identifier"])
    }

    override fun read(instance: CustomPacketEvent, key: String): OpenResult {
        return when(key) {
            "identifier" -> OpenResult.successful(instance.identifier)
            "data" -> OpenResult.successful(instance.data)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: CustomPacketEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
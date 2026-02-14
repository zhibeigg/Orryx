package org.gitee.orryx.core.station.triggers.arcartx

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import priv.seventeen.artist.arcartx.event.client.ClientCustomPacketEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXCustomPacketTrigger: AbstractPropertyEventTrigger<ClientCustomPacketEvent>("ArcartX Custom Packet") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .addParm(Type.STRING, "id", "包ID")
            .addParm(Type.STRING, "data", "包数据")
            .addSpecialKey(Type.STRING, "Ids", "包ID，用于过滤")
            .description("玩家ArcartX自定义发包")

    override val clazz
        get() = ClientCustomPacketEvent::class.java

    override val specialKeys: Array<String> = arrayOf("Ids")

    override fun onJoin(event: ClientCustomPacketEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: ClientCustomPacketEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && (map["Ids"] == null || event.id == map["Ids"])
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientCustomPacketEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player) && (map["Ids"] == null || event.id == map["Ids"])
    }

    override fun read(instance: ClientCustomPacketEvent, key: String): OpenResult {
        return when(key) {
            "id" -> OpenResult.successful(instance.id)
            "data" -> OpenResult.successful(instance.data.joinToString(","))
            "args" -> OpenResult.successful(instance.data)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ClientCustomPacketEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

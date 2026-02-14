package org.gitee.orryx.core.station.triggers.arcartx

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import priv.seventeen.artist.arcartx.event.client.ClientKeyGroupPressEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXKeyGroupPressTrigger: AbstractPropertyEventTrigger<ClientKeyGroupPressEvent>("ArcartX Key Group Press") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .addParm(Type.STRING, "group", "按键组ID")
            .addSpecialKey(Type.STRING, "Groups", "按键组，可写列表/单个")
            .description("玩家按下按键组事件")

    override val clazz
        get() = ClientKeyGroupPressEvent::class.java

    override val specialKeys = arrayOf("Groups")

    override fun onJoin(event: ClientKeyGroupPressEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: ClientKeyGroupPressEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["Groups"] as? List<*>)?.contains(event.groupID) ?: (map["groups"] == event.groupID))
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientKeyGroupPressEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["Groups"] as? List<*>)?.contains(event.groupID) ?: (map["groups"] == event.groupID))
    }

    override fun read(instance: ClientKeyGroupPressEvent, key: String): OpenResult {
        return when(key) {
            "group" -> OpenResult.successful(instance.groupID)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ClientKeyGroupPressEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

package org.gitee.orryx.core.station.triggers.arcartx

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import priv.seventeen.artist.arcartx.event.client.ClientSimpleKeyReleaseEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXSimpleKeyReleaseTrigger: AbstractPropertyEventTrigger<ClientSimpleKeyReleaseEvent>("ArcartX Simple Key Release") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .addParm(Type.STRING, "key", "释放的按键")
            .addSpecialKey(Type.STRING, "Keys", "按键，可写列表/单个")
            .description("玩家释放简单按键事件")

    override val clazz
        get() = ClientSimpleKeyReleaseEvent::class.java

    override val specialKeys = arrayOf("Keys")

    override fun onJoin(event: ClientSimpleKeyReleaseEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: ClientSimpleKeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["Keys"] as? List<*>)?.contains(event.keyName) ?: (map["keys"] == event.keyName))
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientSimpleKeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["Keys"] as? List<*>)?.contains(event.keyName) ?: (map["keys"] == event.keyName))
    }

    override fun read(instance: ClientSimpleKeyReleaseEvent, key: String): OpenResult {
        return when(key) {
            "key" -> OpenResult.successful(instance.keyName)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ClientSimpleKeyReleaseEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

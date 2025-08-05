package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyReleaseEvent
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
object DragonKeyReleaseTrigger: AbstractPropertyPlayerEventTrigger<KeyReleaseEvent>("Dragon Key Release") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "key", "释放的按键")
            .addSpecialKey(Type.STRING, "Keys", "按键，可写列表/单个")
            .description("玩家释放按键事件")

    override val clazz
        get() = KeyReleaseEvent::class.java

    override val specialKeys = arrayOf("Keys")

    override fun onCheck(station: IStation, event: KeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["Keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override fun onCheck(pipeTask: IPipeTask, event: KeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["Keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override fun read(instance: KeyReleaseEvent, key: String): OpenResult {
        return when(key) {
            "key" -> OpenResult.successful(instance.key)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: KeyReleaseEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
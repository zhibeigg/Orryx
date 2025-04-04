package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyReleaseEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object DragonKeyReleaseTrigger: AbstractPlayerEventTrigger<KeyReleaseEvent>() {

    override val event = "Dragon Key Release"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "key", "释放的按键")
            .addSpecialKey(Type.STRING, "keys", "按键，可写列表/单个")
            .description("玩家释放按键事件")

    override val clazz
        get() = KeyReleaseEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onCheck(station: IStation, event: KeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override fun onCheck(pipeTask: IPipeTask, event: KeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override fun onStart(context: ScriptContext, event: KeyReleaseEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["key"] = event.key
    }

}
package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.bukkit.AbstractEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object DragonKeyPressTrigger: AbstractEventTrigger<KeyPressEvent>() {

    override val event = "dragon key press"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "key", "按下的按键")
            .description("玩家按下按键事件（龙核）")

    override val clazz
        get() = KeyPressEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onCheck(pipeTask: IPipeTask, event: KeyPressEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override fun onStart(context: ScriptContext, event: KeyPressEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["key"] = event.key
    }

}
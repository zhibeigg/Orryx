package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyReleaseEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.bukkit.AbstractEventTrigger

@Plugin("DragonCore")
object DragonKeyReleaseTrigger: AbstractEventTrigger<KeyReleaseEvent>() {

    override val event = "dragon key release"

    override val clazz
        get() = KeyReleaseEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onCheck(pipeTask: IPipeTask, event: KeyReleaseEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

}
package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.bukkit.AbstractEventTrigger

@Plugin("DragonCore")
object DragonKeyPressTrigger: AbstractEventTrigger<KeyPressEvent>() {

    override val event = "dragon key press"

    override val clazz
        get() = KeyPressEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onCheck(pipeTask: IPipeTask, event: KeyPressEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

}
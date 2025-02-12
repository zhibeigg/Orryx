package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.event.KeyPressEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.bukkit.AbstractEventTrigger

object DragonKeyPressTrigger: AbstractEventTrigger<KeyPressEvent>() {

    override val event: String
        get() = "dragon key press"

    override val clazz: Class<KeyPressEvent>
        get() = KeyPressEvent::class.java

    override fun onCheck(pipeTask: IPipeTask, event: KeyPressEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.key) ?: (map["keys"] == event.key))
    }

    override val specialKeys: Array<String>
        get() = arrayOf("keys")

}
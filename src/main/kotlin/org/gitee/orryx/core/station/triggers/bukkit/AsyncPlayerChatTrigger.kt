package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.AsyncPlayerChatEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import taboolib.module.kether.ScriptContext
import kotlin.reflect.KClass

object AsyncPlayerChatTrigger: AbstractEventTrigger<AsyncPlayerChatEvent>() {

    override val event: String = "Async Player Chat"

    override val clazz: KClass<AsyncPlayerChatEvent>
        get() = AsyncPlayerChatEvent::class

    override fun onCheck(pipeTask: IPipeTask, event: AsyncPlayerChatEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: AsyncPlayerChatEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["message"] = event.message
        context["format"] = event.format
        context["recipients"] = event.recipients
    }

}
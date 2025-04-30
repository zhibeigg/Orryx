package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.AsyncPlayerChatPreviewEvent
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.module.kether.ScriptContext

object AsyncPlayerChatPreviewTrigger: AbstractPlayerEventTrigger<AsyncPlayerChatPreviewEvent>() {

    override val event: String = "Async Player Chat Preview"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "message", "消息")
            .addParm(Type.STRING, "format", "消息格式")
            .addParm(Type.CONTAINER, "recipients", "能看到这条消息的玩家")
            .description("异步玩家格式化聊天预览")

    override val clazz
        get() = AsyncPlayerChatPreviewEvent::class.java

    override fun onStart(context: ScriptContext, event: AsyncPlayerChatPreviewEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["message"] = event.message
        context["format"] = event.format
        context["recipients"] = Container(event.recipients.mapTo(mutableSetOf()) { it.toTarget() })
    }
}
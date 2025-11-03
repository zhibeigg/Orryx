package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.AsyncPlayerChatEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object AsyncPlayerChatTrigger: AbstractPropertyPlayerEventTrigger<AsyncPlayerChatEvent>("Async Player Chat") {

    override val event: String = "Async Player Chat"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "message", "消息")
            .addParm(Type.STRING, "format", "消息格式")
            .addParm(Type.CONTAINER, "recipients", "能看到这条消息的玩家")
            .description("异步玩家聊天事件触发器")

    override val clazz: java
        get() = AsyncPlayerChatEvent::class.java

    override fun read(instance: AsyncPlayerChatEvent, key: String): OpenResult {
        return when(key) {
            "message" -> OpenResult.successful(instance.message)
            "format" -> OpenResult.successful(instance.format)
            "recipients" -> OpenResult.successful(instance.recipients)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: AsyncPlayerChatEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "message" -> {
                instance.message = value.toString()
                OpenResult.successful()
            }
            "format" -> {
                instance.format = value.toString()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import eos.moe.armourers.ev
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatPreviewEvent
import org.gitee.orryx.core.container.Container
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object AsyncPlayerChatPreviewTrigger: AbstractPropertyPlayerEventTrigger<AsyncPlayerChatPreviewEvent>("Async Player Chat Preview") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "message", "消息")
            .addParm(Type.STRING, "format", "消息格式")
            .addParm(Type.CONTAINER, "recipients", "能看到这条消息的玩家")
            .description("异步玩家格式化聊天预览")

    override val clazz
        get() = AsyncPlayerChatPreviewEvent::class.java

    override fun read(instance: AsyncPlayerChatPreviewEvent, key: String): OpenResult {
        return when(key) {
            "message" -> OpenResult.successful(instance.message)
            "format" -> OpenResult.successful(instance.format)
            "recipients" -> OpenResult.successful(instance.recipients)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: AsyncPlayerChatPreviewEvent, key: String, value: Any?): OpenResult {
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
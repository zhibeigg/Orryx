package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChatTabCompleteEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerChatTabCompleteTrigger: AbstractPropertyPlayerEventTrigger<PlayerChatTabCompleteEvent>("Player Chat Tab Complete") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "lastToken", "获取被补全消息的最后一个'标记'")
            .addParm(Type.STRING, "chatMessage", "获取将被补全的聊天消息")
            .addParm(Type.ITERABLE, "tabCompletions", "获取所有补全项集合")
            .description("当玩家尝试补全聊天消息时触发")

    override val clazz: java
        get() = PlayerChatTabCompleteEvent::class.java

    override fun read(instance: PlayerChatTabCompleteEvent, key: String): OpenResult {
        return when(key) {
            "lastToken" -> OpenResult.successful(instance.lastToken)
            "chatMessage" -> OpenResult.successful(instance.chatMessage)
            "tabCompletions" -> OpenResult.successful(instance.tabCompletions)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerChatTabCompleteEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChatTabCompleteEvent
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerChatTabCompleteTrigger: AbstractPlayerEventTrigger<PlayerChatTabCompleteEvent>() {

    override val event: String = "Player Chat Tab Complete"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "lastToken", "获取被补全消息的最后一个'标记'")
            .addParm(Type.STRING, "chatMessage", "获取将被补全的聊天消息")
            .addParm(Type.ITERABLE, "tabCompletions", "获取所有补全项集合")
            .description("当玩家尝试补全聊天消息时触发")

    override val clazz
        get() = PlayerChatTabCompleteEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerChatTabCompleteEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["lastToken"] = event.lastToken
        context["chatMessage"] = event.chatMessage
        context["tabCompletions"] = event.tabCompletions
    }

}
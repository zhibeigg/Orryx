package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerCommandSendEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerCommandSendTrigger: AbstractPlayerEventTrigger<PlayerCommandSendEvent>() {

    override val event: String = "Player Command Send"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITERABLE, "commands", "返回将发送给客户端的所有顶级命令的可变集合")
            .description("当服务器可用命令列表发送给玩家时触发")

    override val clazz
        get() = PlayerCommandSendEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerCommandSendEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["commands"] = event.commands
    }
}
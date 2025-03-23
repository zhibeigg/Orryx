package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerCommandSendEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerCommandSendTrigger: AbstractEventTrigger<PlayerCommandSendEvent>() {

    override val event: String = "Player Command Send"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITERABLE, "commands", "返回将发送给客户端的所有顶级命令的可变集合")
            .description("当服务器可用命令列表发送给玩家时触发")

    override val clazz
        get() = PlayerCommandSendEvent::class.java

    override fun onJoin(event: PlayerCommandSendEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerCommandSendEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerCommandSendEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["commands"] = event.commands
    }

}
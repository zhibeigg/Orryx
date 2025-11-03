package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerCommandSendEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerCommandSendTrigger: AbstractPropertyPlayerEventTrigger<PlayerCommandSendEvent>("Player Command Send") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.ITERABLE, "commands", "返回将发送给客户端的所有顶级命令的可变集合")
            .description("当服务器可用命令列表发送给玩家时触发")

    override val clazz: java
        get() = PlayerCommandSendEvent::class.java

    override fun read(instance: PlayerCommandSendEvent, key: String): OpenResult {
        return when(key) {
            "commands" -> OpenResult.successful(instance.commands)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerCommandSendEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
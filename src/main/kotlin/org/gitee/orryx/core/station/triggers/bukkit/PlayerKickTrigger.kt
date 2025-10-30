package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerKickEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerKickTrigger: AbstractPropertyPlayerEventTrigger<PlayerKickEvent>("Player Kick") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "leaveMessage", "退出信息")
            .addParm(Type.STRING, "reason", "原因")
            .description("玩家被踢出服务器时触发")

    override val clazz: java
        get() = PlayerKickEvent::class.java

    override fun read(instance: PlayerKickEvent, key: String): OpenResult {
        return when(key) {
            "reason" -> OpenResult.successful(instance.reason)
            "leaveMessage" -> OpenResult.successful(instance.leaveMessage)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerKickEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "reason" -> {
                instance.reason = value.toString()
                OpenResult.successful()
            }
            "leaveMessage" -> {
                instance.leaveMessage = value.toString()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
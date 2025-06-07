package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerJoinEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerJoinTrigger: AbstractPropertyPlayerEventTrigger<PlayerJoinEvent>("Player Join") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "joinMessage", "进入信息")
            .description("玩家进入服务器时触发")

    override val clazz
        get() = PlayerJoinEvent::class.java

    override fun read(instance: PlayerJoinEvent, key: String): OpenResult {
        return when(key) {
            "joinMessage" -> OpenResult.successful(instance.joinMessage)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerJoinEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "joinMessage" -> {
                instance.joinMessage = value.toString()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
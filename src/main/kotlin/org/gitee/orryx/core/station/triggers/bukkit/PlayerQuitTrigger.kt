package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerQuitTrigger: AbstractPropertyPlayerEventTrigger<PlayerQuitEvent>("Player Quit") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "quitMessage", "退出信息")
            .description("玩家退出服务器时触发")

    override val clazz: java
        get() = PlayerQuitEvent::class.java

    override fun read(instance: PlayerQuitEvent, key: String): OpenResult {
        return when(key) {
            "quitMessage" -> OpenResult.successful(instance.quitMessage)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerQuitEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "quitMessage" -> {
                instance.quitMessage = value.toString()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
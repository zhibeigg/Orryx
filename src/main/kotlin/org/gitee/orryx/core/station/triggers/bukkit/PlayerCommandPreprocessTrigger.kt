package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerCommandPreprocessTrigger: AbstractPropertyPlayerEventTrigger<PlayerCommandPreprocessEvent>("Player Command Preprocess") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "message", "命令消息")
            .description("当玩家执行命令前触发")

    override val clazz
        get() = PlayerCommandPreprocessEvent::class.java

    override fun read(instance: PlayerCommandPreprocessEvent, key: String): OpenResult {
        return when(key) {
            "message" -> OpenResult.successful(instance.message)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerCommandPreprocessEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "message" -> {
                instance.message = value.toString()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

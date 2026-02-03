package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChangedWorldEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerChangedWorldTrigger: AbstractPropertyPlayerEventTrigger<PlayerChangedWorldEvent>("Player Changed World") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "from", "从哪个世界来")
            .addParm(Type.STRING, "to", "到哪个世界去")
            .description("当玩家切换到另一个世界时触发")

    override val clazz
        get() = PlayerChangedWorldEvent::class.java

    override fun read(instance: PlayerChangedWorldEvent, key: String): OpenResult {
        return when(key) {
            "from" -> OpenResult.successful(instance.from.name)
            "to" -> OpenResult.successful(instance.player.world.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerChangedWorldEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
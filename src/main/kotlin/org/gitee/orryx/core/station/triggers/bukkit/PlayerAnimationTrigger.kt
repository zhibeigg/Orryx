package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerAnimationEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object PlayerAnimationTrigger: AbstractPropertyPlayerEventTrigger<PlayerAnimationEvent>("Player Animation") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "animation", "动作名")
            .description("玩家动作")

    override val clazz
        get() = PlayerAnimationEvent::class.java

    override fun read(instance: PlayerAnimationEvent, key: String): OpenResult {
        return when(key) {
            "animation" -> OpenResult.successful(instance.animationType.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerAnimationEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
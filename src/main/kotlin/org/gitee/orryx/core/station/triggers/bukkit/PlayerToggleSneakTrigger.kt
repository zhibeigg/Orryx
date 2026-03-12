package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerToggleSneakEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerToggleSneakTrigger: AbstractPropertyPlayerEventTrigger<PlayerToggleSneakEvent>("Player Toggle Sneak") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.BOOLEAN, "isSneaking", "是否正在潜行")
            .description("当玩家切换潜行状态时触发")

    override val clazz
        get() = PlayerToggleSneakEvent::class.java

    override fun read(instance: PlayerToggleSneakEvent, key: String): OpenResult {
        return when(key) {
            "isSneaking" -> OpenResult.successful(instance.isSneaking)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerToggleSneakEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

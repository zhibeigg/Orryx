package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerToggleSprintEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerToggleSprintTrigger: AbstractPropertyPlayerEventTrigger<PlayerToggleSprintEvent>("Player Toggle Sprint") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.BOOLEAN, "isSprinting", "是否正在疾跑")
            .description("当玩家切换疾跑状态时触发")

    override val clazz
        get() = PlayerToggleSprintEvent::class.java

    override fun read(instance: PlayerToggleSprintEvent, key: String): OpenResult {
        return when(key) {
            "isSprinting" -> OpenResult.successful(instance.isSprinting)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerToggleSprintEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

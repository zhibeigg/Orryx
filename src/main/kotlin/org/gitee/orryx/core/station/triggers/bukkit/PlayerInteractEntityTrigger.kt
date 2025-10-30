package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerInteractEntityEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult

object PlayerInteractEntityTrigger: AbstractPropertyPlayerEventTrigger<PlayerInteractEntityEvent>("Player Interact Entity") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "rightClicked", "被点击的实体")
            .description("当玩家点击一个实体时调用")

    override val clazz: java
        get() = PlayerInteractEntityEvent::class.java

    override fun read(instance: PlayerInteractEntityEvent, key: String): OpenResult {
        return when(key) {
            "rightClicked" -> OpenResult.successful(instance.rightClicked.abstract())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerInteractEntityEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
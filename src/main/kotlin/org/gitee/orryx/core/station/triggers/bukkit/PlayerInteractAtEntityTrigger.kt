package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerHideEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult
import taboolib.module.kether.ScriptContext

object PlayerInteractAtEntityTrigger: AbstractPropertyPlayerEventTrigger<PlayerInteractAtEntityEvent>("Player Interact At Entity") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.VECTOR, "clickedPosition", "点击的方向向量")
            .addParm(Type.TARGET, "rightClicked", "被点击的实体")
            .description("当玩家在实体上点击某实体上的某位置时触发")

    override val clazz
        get() = PlayerInteractAtEntityEvent::class.java

    override fun read(instance: PlayerInteractAtEntityEvent, key: String): OpenResult {
        return when(key) {
            "clickedPosition" -> OpenResult.successful(instance.clickedPosition.abstract())
            "rightClicked" -> OpenResult.successful(instance.rightClicked.abstract())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerInteractAtEntityEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
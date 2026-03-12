package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerRespawnEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerRespawnTrigger: AbstractPropertyPlayerEventTrigger<PlayerRespawnEvent>("Player Respawn") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "respawnLocation", "重生位置")
            .addParm(Type.BOOLEAN, "isBedSpawn", "是否为床重生")
            .description("当玩家重生时触发")

    override val clazz
        get() = PlayerRespawnEvent::class.java

    override fun read(instance: PlayerRespawnEvent, key: String): OpenResult {
        return when(key) {
            "respawnLocation" -> OpenResult.successful(instance.respawnLocation.toTarget())
            "isBedSpawn" -> OpenResult.successful(instance.isBedSpawn)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerRespawnEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "respawnLocation" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.respawnLocation = it }
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerGameModeChangeTrigger: AbstractPropertyPlayerEventTrigger<PlayerGameModeChangeEvent>("Player GameMode Change") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "newGameMode", "新的游戏模式：ADVENTURE/CREATIVE/SPECTATOR/SURVIVAL")
            .description("当玩家游戏模式发生变化时触发")

    override val clazz
        get() = PlayerGameModeChangeEvent::class.java

    override fun read(instance: PlayerGameModeChangeEvent, key: String): OpenResult {
        return when(key) {
            "newGameMode" -> OpenResult.successful(instance.newGameMode.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerGameModeChangeEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
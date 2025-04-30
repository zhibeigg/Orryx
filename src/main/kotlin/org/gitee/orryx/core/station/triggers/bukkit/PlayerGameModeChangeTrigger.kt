package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerGameModeChangeTrigger: AbstractPlayerEventTrigger<PlayerGameModeChangeEvent>() {

    override val event: String = "Player GameMode Change"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "newGameMode", "新的游戏模式：ADVENTURE/CREATIVE/SPECTATOR/SURVIVAL")
            .description("当玩家游戏模式发生变化时触发")

    override val clazz
        get() = PlayerGameModeChangeEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerGameModeChangeEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["newGameMode"] = event.newGameMode.name
    }
}
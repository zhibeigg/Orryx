package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerQuitEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerQuitTrigger: AbstractPlayerEventTrigger<PlayerQuitEvent>() {

    override val event: String = "Player Quit"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "quitMessage", "退出信息")
            .description("玩家退出服务器时触发")

    override val clazz
        get() = PlayerQuitEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerQuitEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["quitMessage"] = event.quitMessage
    }

}
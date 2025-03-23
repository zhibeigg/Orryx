package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerAnimationEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerAnimationTrigger: AbstractEventTrigger<PlayerAnimationEvent>() {

    override val event: String = "Player Animation"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "animation", "动作名")
            .description("玩家动作")

    override val clazz
        get() = PlayerAnimationEvent::class.java

    override fun onJoin(event: PlayerAnimationEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerAnimationEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerAnimationEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["animation"] = event.animationType.name
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChangedMainHandEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerChangedMainHandTrigger: AbstractEventTrigger<PlayerChangedMainHandEvent>() {

    override val event: String = "Player Changed MainHand"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "mainHand", "改变后的主手：LEFT/RIGHT")
            .description("当玩家在客户端设置改变主手时触发")

    override val clazz
        get() = PlayerChangedMainHandEvent::class.java

    override fun onJoin(event: PlayerChangedMainHandEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerChangedMainHandEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerChangedMainHandEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["mainHand"] = event.mainHand.name
    }

}
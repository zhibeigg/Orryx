package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChangedMainHandEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerChangedMainHandTrigger: AbstractPlayerEventTrigger<PlayerChangedMainHandEvent>() {

    override val event: String = "Player Changed MainHand"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "mainHand", "改变后的主手：LEFT/RIGHT")
            .description("当玩家在客户端设置改变主手时触发")

    override val clazz
        get() = PlayerChangedMainHandEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerChangedMainHandEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["mainHand"] = event.mainHand.name
    }
}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerChangedMainHandEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult

object PlayerChangedMainHandTrigger: AbstractPropertyPlayerEventTrigger<PlayerChangedMainHandEvent>("Player Changed MainHand") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "mainHand", "改变后的主手：LEFT/RIGHT")
            .description("当玩家在客户端设置改变主手时触发")

    override val clazz: java
        get() = PlayerChangedMainHandEvent::class.java

    override fun read(instance: PlayerChangedMainHandEvent, key: String): OpenResult {
        return when(key) {
            "mainHand" -> OpenResult.successful(instance.mainHand.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerChangedMainHandEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
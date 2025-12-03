package org.gitee.orryx.core.station.triggers.orryx.press

import org.gitee.orryx.api.events.player.press.OrryxPlayerPressTickEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerPressTickTrigger: AbstractPropertyEventTrigger<OrryxPlayerPressTickEvent>("Orryx Player Press Tick") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "skill", "技能（空为普攻）")
            .addParm(Type.LONG, "tick", "已经蓄力时间")
            .addParm(Type.LONG, "maxTick", "蓄力最长时间")
            .addParm(Type.LONG, "period", "周期时间")
            .description("玩家开始蓄力后的周期性事件")

    override val clazz: java
        get() = OrryxPlayerPressTickEvent::class.java

    override fun onJoin(event: OrryxPlayerPressTickEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerPressTickEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerPressTickEvent, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill?.key)
            "tick" -> OpenResult.successful(instance.pressTick)
            "maxTick" -> OpenResult.successful(instance.maxPressTick)
            "period" -> OpenResult.successful(instance.period)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerPressTickEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
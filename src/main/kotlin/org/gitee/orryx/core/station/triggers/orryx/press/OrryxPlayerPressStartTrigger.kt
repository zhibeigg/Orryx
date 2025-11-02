package org.gitee.orryx.core.station.triggers.orryx.press

import org.gitee.orryx.api.events.player.press.OrryxPlayerPressStartEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerPressStartTrigger: AbstractPropertyEventTrigger<OrryxPlayerPressStartEvent>("Orryx Player Press Start") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "skill", "技能（空为普攻）")
            .addParm(Type.LONG, "tick", "蓄力最长时间")
            .description("玩家开始蓄力事件")

    override val clazz: java
        get() = OrryxPlayerPressStartEvent::class.java

    override fun onJoin(event: OrryxPlayerPressStartEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerPressStartEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerPressStartEvent, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill?.key)
            "tick" -> OpenResult.successful(instance.pressTick)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerPressStartEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
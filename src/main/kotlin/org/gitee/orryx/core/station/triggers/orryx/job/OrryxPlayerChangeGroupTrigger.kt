package org.gitee.orryx.core.station.triggers.orryx.job

import org.gitee.orryx.api.events.player.OrryxPlayerChangeGroupEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerChangeGroupTrigger: AbstractPropertyEventTrigger<OrryxPlayerChangeGroupEvents.Pre>("Orryx Player Change Group") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "job", "玩家职业")
            .addParm(Type.ANY, "group", "技能组")
            .description("玩家切换技能组事件")

    override val clazz
        get() = OrryxPlayerChangeGroupEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerChangeGroupEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerChangeGroupEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerChangeGroupEvents.Pre, key: String): OpenResult {
        return when (key) {
            "job" -> OpenResult.successful(instance.job)
            "group" -> OpenResult.successful(instance.group)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerChangeGroupEvents.Pre, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
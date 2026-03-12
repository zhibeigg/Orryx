package org.gitee.orryx.core.station.triggers.orryx.job

import org.gitee.orryx.api.events.player.job.OrryxPlayerJobClearEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerJobClearTrigger: AbstractPropertyEventTrigger<OrryxPlayerJobClearEvents.Pre>("Orryx Player Job Clear") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "job", "玩家职业")
            .description("玩家职业清除事件")

    override val clazz
        get() = OrryxPlayerJobClearEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerJobClearEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerJobClearEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerJobClearEvents.Pre, key: String): OpenResult {
        return when (key) {
            "job" -> OpenResult.successful(instance.job)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerJobClearEvents.Pre, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
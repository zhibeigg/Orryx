package org.gitee.orryx.core.station.triggers.orryx.level

import org.gitee.orryx.api.events.player.job.OrryxPlayerJobLevelEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerLevelDownTrigger: AbstractPropertyEventTrigger<OrryxPlayerJobLevelEvents.Down>("Orryx Player Level Down") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "level", "变化等级")
            .description("玩家等级下降事件")

    override val clazz: java
        get() = OrryxPlayerJobLevelEvents.Down::class.java

    override fun onJoin(event: OrryxPlayerJobLevelEvents.Down, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerJobLevelEvents.Down, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerJobLevelEvents.Down, key: String): OpenResult {
        return when (key) {
            "level" -> OpenResult.successful(instance.downLevel)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerJobLevelEvents.Down, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
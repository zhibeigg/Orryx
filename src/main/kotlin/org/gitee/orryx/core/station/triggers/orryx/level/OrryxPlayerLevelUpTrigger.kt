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

object OrryxPlayerLevelUpTrigger: AbstractPropertyEventTrigger<OrryxPlayerJobLevelEvents.Up.Post>("Orryx Player Level Up") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "level", "变化等级")
            .description("玩家等级上升事件")

    override val clazz
        get() = OrryxPlayerJobLevelEvents.Up.Post::class.java

    override fun onJoin(event: OrryxPlayerJobLevelEvents.Up.Post, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerJobLevelEvents.Up.Post, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerJobLevelEvents.Up.Post, key: String): OpenResult {
        return when (key) {
            "level" -> OpenResult.successful(instance.upLevel)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerJobLevelEvents.Up.Post, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
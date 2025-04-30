package org.gitee.orryx.core.station.triggers.orryx.point

import org.gitee.orryx.api.events.player.OrryxPlayerPointEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint

object OrryxPlayerPointUpTrigger: AbstractPropertyEventTrigger<OrryxPlayerPointEvents.Up.Pre>("Orryx Player Point Up") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "point", "变化技能点")
            .description("玩家技能点上升事件")

    override val clazz
        get() = OrryxPlayerPointEvents.Up.Pre::class.java

    override fun onJoin(event: OrryxPlayerPointEvents.Up.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerPointEvents.Up.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerPointEvents.Up.Pre, key: String): OpenResult {
        return when (key) {
            "point" -> OpenResult.successful(instance.point)
            "profile" -> OpenResult.successful(instance.profile)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerPointEvents.Up.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "point" -> {
                instance.point = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
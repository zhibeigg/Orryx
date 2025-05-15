package org.gitee.orryx.core.station.triggers.orryx.exp

import org.gitee.orryx.api.events.player.job.OrryxPlayerJobExperienceEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint

object OrryxPlayerExpDownTrigger: AbstractPropertyEventTrigger<OrryxPlayerJobExperienceEvents.Down>("Orryx Player Exp Down") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "exp/experience", "变化经验")
            .description("玩家经验下降事件")

    override val clazz
        get() = OrryxPlayerJobExperienceEvents.Down::class.java

    override fun onJoin(event: OrryxPlayerJobExperienceEvents.Down, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerJobExperienceEvents.Down, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerJobExperienceEvents.Down, key: String): OpenResult {
        return when (key) {
            "exp", "experience" -> OpenResult.successful(instance.downExperience)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerJobExperienceEvents.Down, key: String, value: Any?): OpenResult {
        return when (key) {
            "exp", "experience" -> {
                instance.downExperience = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
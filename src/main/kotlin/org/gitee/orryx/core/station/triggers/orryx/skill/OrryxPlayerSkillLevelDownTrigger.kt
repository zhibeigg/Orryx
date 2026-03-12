package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillLevelEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint

object OrryxPlayerSkillLevelDownTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillLevelEvents.Down.Pre>("Orryx Player Skill Level Down") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.INT, "level", "降级等级数")
            .description("玩家技能降级事件")

    override val clazz
        get() = OrryxPlayerSkillLevelEvents.Down.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillLevelEvents.Down.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillLevelEvents.Down.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillLevelEvents.Down.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "level" -> OpenResult.successful(instance.downLevel)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillLevelEvents.Down.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "level" -> {
                instance.downLevel = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
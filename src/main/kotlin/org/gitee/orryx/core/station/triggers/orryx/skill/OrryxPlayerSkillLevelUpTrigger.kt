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

object OrryxPlayerSkillLevelUpTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillLevelEvents.Up.Pre>("Orryx Player Skill Level Up") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.INT, "level", "升级等级数")
            .description("玩家技能升级事件")

    override val clazz
        get() = OrryxPlayerSkillLevelEvents.Up.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillLevelEvents.Up.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillLevelEvents.Up.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillLevelEvents.Up.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "level" -> OpenResult.successful(instance.upLevel)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillLevelEvents.Up.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "level" -> {
                instance.upLevel = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxClearSkillLevelAndBackPointEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxClearSkillLevelTrigger: AbstractPropertyEventTrigger<OrryxClearSkillLevelAndBackPointEvent>("Orryx Clear Skill Level") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .description("清除技能等级并返还点数事件")

    override val clazz
        get() = OrryxClearSkillLevelAndBackPointEvent::class.java

    override fun onJoin(event: OrryxClearSkillLevelAndBackPointEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxClearSkillLevelAndBackPointEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxClearSkillLevelAndBackPointEvent, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxClearSkillLevelAndBackPointEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxClearAllSkillLevelAndBackPointEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxClearAllSkillLevelTrigger: AbstractPropertyEventTrigger<OrryxClearAllSkillLevelAndBackPointEvent>("Orryx Clear All Skill Level") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "job", "玩家职业")
            .description("清除所有技能等级并返还点数事件")

    override val clazz
        get() = OrryxClearAllSkillLevelAndBackPointEvent::class.java

    override fun onJoin(event: OrryxClearAllSkillLevelAndBackPointEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxClearAllSkillLevelAndBackPointEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxClearAllSkillLevelAndBackPointEvent, key: String): OpenResult {
        return when (key) {
            "job" -> OpenResult.successful(instance.job)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxClearAllSkillLevelAndBackPointEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
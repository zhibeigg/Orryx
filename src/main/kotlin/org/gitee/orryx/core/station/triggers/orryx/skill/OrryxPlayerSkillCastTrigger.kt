package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCastEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerSkillCastTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillCastEvents.Cast>("Orryx Player Skill Cast") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.LONG, "skillParameter", "技能参数上下文")
            .description("玩家技能释放事件")

    override val clazz: java
        get() = OrryxPlayerSkillCastEvents.Cast::class.java

    override fun onJoin(event: OrryxPlayerSkillCastEvents.Cast, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillCastEvents.Cast, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillCastEvents.Cast, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "skillParameter" -> OpenResult.successful(instance.skillParameter)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillCastEvents.Cast, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
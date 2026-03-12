package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillClearEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerSkillClearTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillClearEvents.Pre>("Orryx Player Skill Clear") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .description("玩家技能清除事件")

    override val clazz
        get() = OrryxPlayerSkillClearEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillClearEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillClearEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillClearEvents.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillClearEvents.Pre, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
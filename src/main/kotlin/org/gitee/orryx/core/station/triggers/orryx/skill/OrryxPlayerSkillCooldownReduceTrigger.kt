package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillCooldownEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.clong

object OrryxPlayerSkillCooldownReduceTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillCooldownEvents.Reduce.Pre>("Orryx Player Skill Cooldown Reduce") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.LONG, "amount", "缩减的!毫秒!数值")
            .description("玩家技能冷却缩减事件")

    override val clazz
        get() = OrryxPlayerSkillCooldownEvents.Reduce.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillCooldownEvents.Reduce.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillCooldownEvents.Reduce.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillCooldownEvents.Reduce.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "amount" -> OpenResult.successful(instance.amount)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillCooldownEvents.Reduce.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "amount" -> {
                instance.amount = value.clong
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
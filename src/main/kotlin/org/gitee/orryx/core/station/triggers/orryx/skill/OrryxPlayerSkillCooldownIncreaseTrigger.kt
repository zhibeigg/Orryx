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

object OrryxPlayerSkillCooldownIncreaseTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillCooldownEvents.Increase.Pre>("Orryx Player Skill Cooldown Increase") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.LONG, "amount", "增加的!毫秒!数值")
            .description("玩家技能冷却增加事件")

    override val clazz: java
        get() = OrryxPlayerSkillCooldownEvents.Increase.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillCooldownEvents.Increase.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillCooldownEvents.Increase.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillCooldownEvents.Increase.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "amount" -> OpenResult.successful(instance.amount)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillCooldownEvents.Increase.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "amount" -> {
                instance.amount = value.clong
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
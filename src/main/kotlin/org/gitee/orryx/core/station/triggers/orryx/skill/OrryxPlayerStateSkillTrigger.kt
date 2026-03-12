package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.state.OrryxPlayerStateSkillEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.clong

object OrryxPlayerStateSkillTrigger: AbstractPropertyEventTrigger<OrryxPlayerStateSkillEvents.Pre>("Orryx Player State Skill") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skillParameter", "技能参数上下文")
            .addParm(Type.LONG, "silence", "沉默时长")
            .addParm(Type.ANY, "state", "技能状态")
            .description("玩家状态技能触发事件")

    override val clazz
        get() = OrryxPlayerStateSkillEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerStateSkillEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerStateSkillEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerStateSkillEvents.Pre, key: String): OpenResult {
        return when (key) {
            "skillParameter" -> OpenResult.successful(instance.skillParameter)
            "silence" -> OpenResult.successful(instance.silence)
            "state" -> OpenResult.successful(instance.state)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerStateSkillEvents.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "silence" -> {
                instance.silence = value.clong
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
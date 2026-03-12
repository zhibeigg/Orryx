package org.gitee.orryx.core.station.triggers.orryx.skill

import org.gitee.orryx.api.events.player.skill.OrryxPlayerSkillUnBindKeyEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerSkillUnBindKeyTrigger: AbstractPropertyEventTrigger<OrryxPlayerSkillUnBindKeyEvent.Pre>("Orryx Player Skill UnBind Key") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "skill", "玩家技能")
            .addParm(Type.ANY, "group", "按键组")
            .description("玩家技能解绑按键事件")

    override val clazz
        get() = OrryxPlayerSkillUnBindKeyEvent.Pre::class.java

    override fun onJoin(event: OrryxPlayerSkillUnBindKeyEvent.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSkillUnBindKeyEvent.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSkillUnBindKeyEvent.Pre, key: String): OpenResult {
        return when (key) {
            "skill" -> OpenResult.successful(instance.skill)
            "group" -> OpenResult.successful(instance.group)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSkillUnBindKeyEvent.Pre, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
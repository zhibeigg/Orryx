package org.gitee.orryx.core.station.triggers.orryx.spirit

import org.gitee.orryx.api.events.player.OrryxPlayerSpiritEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerSpiritHealTrigger: AbstractPropertyEventTrigger<OrryxPlayerSpiritEvents.Heal>("Orryx Player Spirit Heal") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "spirit", "变化精力值")
            .description("玩家精力值上升事件")

    override val clazz
        get() = OrryxPlayerSpiritEvents.Heal::class.java

    override fun onJoin(event: OrryxPlayerSpiritEvents.Heal, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerSpiritEvents.Heal, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerSpiritEvents.Heal, key: String): OpenResult {
        return when (key) {
            "spirit" -> OpenResult.successful(instance.healSpirit)
            "profile" -> OpenResult.successful(instance.profile)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerSpiritEvents.Heal, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
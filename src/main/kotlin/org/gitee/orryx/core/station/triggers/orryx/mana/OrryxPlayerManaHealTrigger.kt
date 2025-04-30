package org.gitee.orryx.core.station.triggers.orryx.mana

import org.gitee.orryx.api.events.player.OrryxPlayerManaEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.KetherProperty

object OrryxPlayerManaHealTrigger: AbstractPropertyEventTrigger<OrryxPlayerManaEvents.Heal.Pre>("Orryx Player Mana Heal") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "mana", "变化蓝量")
            .description("玩家蓝量指令回满事件")

    override val clazz
        get() = OrryxPlayerManaEvents.Heal.Pre::class.java

    override fun onJoin(event: OrryxPlayerManaEvents.Heal.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerManaEvents.Heal.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerManaEvents.Heal.Pre, key: String): OpenResult {
        return when (key) {
            "mana" -> OpenResult.successful(instance.healMana)
            "profile" -> OpenResult.successful(instance.profile)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerManaEvents.Heal.Pre, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
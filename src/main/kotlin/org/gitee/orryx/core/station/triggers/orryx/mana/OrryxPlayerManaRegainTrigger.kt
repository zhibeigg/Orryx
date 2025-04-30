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
import taboolib.common5.cdouble
import taboolib.module.kether.KetherProperty

object OrryxPlayerManaRegainTrigger: AbstractPropertyEventTrigger<OrryxPlayerManaEvents.Regain.Pre>("Orryx Player Mana Regain") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.DOUBLE, "mana", "变化蓝量")
            .description("玩家蓝量自然恢复事件")

    override val clazz
        get() = OrryxPlayerManaEvents.Regain.Pre::class.java

    override fun onJoin(event: OrryxPlayerManaEvents.Regain.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerManaEvents.Regain.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerManaEvents.Regain.Pre, key: String): OpenResult {
        return when (key) {
            "mana" -> OpenResult.successful(instance.regainMana)
            "profile" -> OpenResult.successful(instance.profile)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerManaEvents.Regain.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "mana" -> {
                instance.regainMana = value.cdouble
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
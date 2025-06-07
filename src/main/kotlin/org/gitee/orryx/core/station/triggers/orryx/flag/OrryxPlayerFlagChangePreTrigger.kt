package org.gitee.orryx.core.station.triggers.orryx.flag

import org.gitee.orryx.api.events.player.OrryxPlayerFlagChangeEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

object OrryxPlayerFlagChangePreTrigger: AbstractPropertyEventTrigger<OrryxPlayerFlagChangeEvents.Pre>("Orryx Player Flag Change Pre") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "key/flagName", "flag的键")
            .addParm(Type.ANY, "oldFlag", "旧的flag")
            .addParm(Type.ANY, "newFlag", "新的flag")
            .description("玩家Flag变化事件")

    override val clazz
        get() = OrryxPlayerFlagChangeEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerFlagChangeEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerFlagChangeEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerFlagChangeEvents.Pre, key: String): OpenResult {
        return when (key) {
            "key", "flagName" -> OpenResult.successful(instance.flagName)
            "oldFlag" -> OpenResult.successful(instance.oldFlag?.value)
            "newFlag" -> OpenResult.successful(instance.newFlag?.value)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerFlagChangeEvents.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            else -> OpenResult.failed()
        }
    }
}
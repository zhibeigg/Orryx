package org.gitee.orryx.core.station.triggers.orryx.flag

import org.gitee.orryx.api.events.OrryxGlobalFlagChangeEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.console

object OrryxGlobalFlagChangePreTrigger: AbstractPropertyEventTrigger<OrryxGlobalFlagChangeEvents.Pre>("Orryx Global Flag Change Pre") {

    override val wiki: Trigger
        get() = Trigger.Companion.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "key/flagName", "flag的键")
            .addParm(Type.ANY, "oldFlag", "旧的flag")
            .addParm(Type.ANY, "newFlag", "新的flag")
            .description("全局Flag变化事件")

    override val clazz
        get() = OrryxGlobalFlagChangeEvents.Pre::class.java

    override fun onJoin(event: OrryxGlobalFlagChangeEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return console()
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxGlobalFlagChangeEvents.Pre, map: Map<String, Any?>): Boolean {
        return true
    }

    override fun read(instance: OrryxGlobalFlagChangeEvents.Pre, key: String): OpenResult {
        return when (key) {
            "key", "flagName" -> OpenResult.successful(instance.flagName)
            "oldFlag" -> OpenResult.successful(instance.oldFlag?.value)
            "newFlag" -> OpenResult.successful(instance.newFlag?.value)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxGlobalFlagChangeEvents.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            else -> OpenResult.failed()
        }
    }
}
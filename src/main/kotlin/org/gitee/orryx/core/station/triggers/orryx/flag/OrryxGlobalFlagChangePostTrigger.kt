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

object OrryxGlobalFlagChangePostTrigger: AbstractPropertyEventTrigger<OrryxGlobalFlagChangeEvents.Post>("Orryx Global Flag Change Post") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "key/flagName", "flag的键")
            .addParm(Type.ANY, "oldFlag", "旧的flag")
            .addParm(Type.ANY, "newFlag", "新的flag")
            .description("全局Flag变化事件")

    override val clazz: java
        get() = OrryxGlobalFlagChangeEvents.Post::class.java

    override fun onJoin(event: OrryxGlobalFlagChangeEvents.Post, map: Map<String, Any?>): ProxyCommandSender {
        return console()
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxGlobalFlagChangeEvents.Post, map: Map<String, Any?>): Boolean {
        return true
    }

    override fun read(instance: OrryxGlobalFlagChangeEvents.Post, key: String): OpenResult {
        return when (key) {
            "key", "flagName" -> OpenResult.successful(instance.flagName)
            "oldFlag" -> OpenResult.successful(instance.oldFlag?.value)
            "newFlag" -> OpenResult.successful(instance.newFlag?.value)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxGlobalFlagChangeEvents.Post, key: String, value: Any?): OpenResult {
        return when (key) {
            else -> OpenResult.failed()
        }
    }
}
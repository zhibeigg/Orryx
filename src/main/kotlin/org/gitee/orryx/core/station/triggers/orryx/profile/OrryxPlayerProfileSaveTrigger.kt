package org.gitee.orryx.core.station.triggers.orryx.profile

import org.gitee.orryx.api.events.player.OrryxPlayerProfileSaveEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cbool

object OrryxPlayerProfileSaveTrigger: AbstractPropertyEventTrigger<OrryxPlayerProfileSaveEvents.Pre>("Orryx Player Profile Save") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.ANY, "profile", "玩家存档")
            .addParm(Type.BOOLEAN, "async", "是否异步")
            .addParm(Type.BOOLEAN, "remove", "是否移除")
            .description("玩家存档保存事件")

    override val clazz
        get() = OrryxPlayerProfileSaveEvents.Pre::class.java

    override fun onJoin(event: OrryxPlayerProfileSaveEvents.Pre, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerProfileSaveEvents.Pre, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerProfileSaveEvents.Pre, key: String): OpenResult {
        return when (key) {
            "profile" -> OpenResult.successful(instance.profile)
            "async" -> OpenResult.successful(instance.async)
            "remove" -> OpenResult.successful(instance.remove)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerProfileSaveEvents.Pre, key: String, value: Any?): OpenResult {
        return when (key) {
            "async" -> {
                instance.async = value.cbool
                OpenResult.successful()
            }
            "remove" -> {
                instance.remove = value.cbool
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
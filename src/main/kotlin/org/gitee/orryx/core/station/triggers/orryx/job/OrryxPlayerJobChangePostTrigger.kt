package org.gitee.orryx.core.station.triggers.orryx.job

import org.gitee.orryx.api.events.player.job.OrryxPlayerJobChangeEvents
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.job
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.orNull

object OrryxPlayerJobChangePostTrigger: AbstractPropertyEventTrigger<OrryxPlayerJobChangeEvents.Post>("Orryx Player Job Change Post") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ORRYX, event)
            .addParm(Type.STRING, "from/old", "老职业")
            .addParm(Type.STRING, "to/new", "新职业")
            .description("玩家职业改变后事件")

    override val clazz: java
        get() = OrryxPlayerJobChangeEvents.Post::class.java

    override fun onJoin(event: OrryxPlayerJobChangeEvents.Post, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: OrryxPlayerJobChangeEvents.Post, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: OrryxPlayerJobChangeEvents.Post, key: String): OpenResult {
        return when (key) {
            "from", "old" -> OpenResult.successful(instance.player.job().orNull()?.key)
            "to", "new" -> OpenResult.successful(instance.job.key)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: OrryxPlayerJobChangeEvents.Post, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
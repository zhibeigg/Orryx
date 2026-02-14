package org.gitee.orryx.core.station.triggers.arcartx

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import priv.seventeen.artist.arcartx.event.client.ClientChannelEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXClientChannelTrigger: AbstractPropertyEventTrigger<ClientChannelEvent>("ArcartX Client Channel") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .description("玩家ArcartX客户端通道连接")

    override val clazz
        get() = ClientChannelEvent::class.java

    override fun onJoin(event: ClientChannelEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientChannelEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: ClientChannelEvent, key: String): OpenResult {
        return OpenResult.failed()
    }

    override fun write(instance: ClientChannelEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

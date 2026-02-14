package org.gitee.orryx.core.station.triggers.arcartx

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import priv.seventeen.artist.arcartx.event.client.ClientMouseClickEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXMouseClickTrigger: AbstractPropertyEventTrigger<ClientMouseClickEvent>("ArcartX Mouse Click") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .addParm(Type.INT, "button", "鼠标按键")
            .addParm(Type.INT, "action", "动作类型")
            .description("玩家鼠标点击事件")

    override val clazz
        get() = ClientMouseClickEvent::class.java

    override fun onJoin(event: ClientMouseClickEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientMouseClickEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun read(instance: ClientMouseClickEvent, key: String): OpenResult {
        return when(key) {
            "button" -> OpenResult.successful(instance.button)
            "action" -> OpenResult.successful(instance.action)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ClientMouseClickEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

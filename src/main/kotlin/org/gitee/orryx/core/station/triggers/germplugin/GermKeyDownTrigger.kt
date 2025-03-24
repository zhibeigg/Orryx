package org.gitee.orryx.core.station.triggers.germplugin

import com.germ.germplugin.api.event.GermKeyDownEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.bukkit.AbstractEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

@Plugin("GermPlugin")
object GermKeyDownTrigger: AbstractEventTrigger<GermKeyDownEvent>() {

    override val event = "Germ Key Down"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.GERM_PLUGIN, event)
            .addParm(Type.STRING, "key", "按下的按键")
            .description("玩家按下按键事件")

    override val clazz
        get() = GermKeyDownEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onJoin(event: GermKeyDownEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: GermKeyDownEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun onCheck(pipeTask: IPipeTask, event: GermKeyDownEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun onStart(context: ScriptContext, event: GermKeyDownEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["key"] = event.keyType.simpleKey
    }

}
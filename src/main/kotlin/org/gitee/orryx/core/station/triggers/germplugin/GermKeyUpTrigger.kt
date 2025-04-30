package org.gitee.orryx.core.station.triggers.germplugin

import com.germ.germplugin.api.event.GermKeyUpEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

@Plugin("GermPlugin")
object GermKeyUpTrigger: AbstractPropertyEventTrigger<GermKeyUpEvent>("Germ Key Up") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.GERM_PLUGIN, event)
            .addParm(Type.STRING, "key", "按下的按键")
            .addSpecialKey(Type.STRING, "keys", "按键，可写列表/单个")
            .description("玩家释放按键事件")

    override val clazz
        get() = GermKeyUpEvent::class.java

    override val specialKeys = arrayOf("keys")

    override fun onJoin(event: GermKeyUpEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(station: IStation, event: GermKeyUpEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && ((map["keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun onCheck(pipeTask: IPipeTask, event: GermKeyUpEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player && ((map["keys"] as? List<*>)?.contains(event.keyType.simpleKey) ?: (map["keys"] == event.keyType.simpleKey))
    }

    override fun onStart(context: ScriptContext, event: GermKeyUpEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["key"] = event.keyType.simpleKey
    }
}
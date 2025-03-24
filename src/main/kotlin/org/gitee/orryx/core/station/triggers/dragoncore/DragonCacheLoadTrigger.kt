package org.gitee.orryx.core.station.triggers.dragoncore

import eos.moe.dragoncore.api.gui.event.CustomPacketEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.bukkit.AbstractPlayerEventTrigger
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object DragonCacheLoadTrigger: AbstractPlayerEventTrigger<CustomPacketEvent>() {

    override val event = "Dragon Cache Load"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .description("玩家龙核缓存加载")

    override val clazz
        get() = CustomPacketEvent::class.java

    override fun onCheck(station: IStation, event: CustomPacketEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.data.size == 1 && event.data[0] == "cache_loaded"
    }

    override fun onCheck(pipeTask: IPipeTask, event: CustomPacketEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player) && event.data.size == 1 && event.data[0] == "cache_loaded"
    }

    override fun onStart(context: ScriptContext, event: CustomPacketEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
    }

}
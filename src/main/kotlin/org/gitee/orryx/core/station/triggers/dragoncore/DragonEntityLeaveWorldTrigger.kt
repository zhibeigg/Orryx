package org.gitee.orryx.core.station.triggers.dragoncore

import com.eatthepath.uuid.FastUUID
import eos.moe.dragoncore.api.event.EntityLeaveWorldEvent
import ink.ptms.adyeshach.core.Adyeshach
import org.bukkit.Bukkit
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.AdyeshachPlugin
import org.gitee.orryx.utils.abstract
import taboolib.module.kether.ScriptContext

@Plugin("DragonCore")
object DragonEntityLeaveWorldTrigger: AbstractPlayerEventTrigger<EntityLeaveWorldEvent>() {

    override val event = "Dragon Entity Leave"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "entityUUID", "实体的UUID")
            .addParm(Type.TARGET, "entity", "实体，不一定存在")
            .description("玩家客户端中有实体离开")

    override val clazz
        get() = EntityLeaveWorldEvent::class.java

    override fun onCheck(pipeTask: IPipeTask, event: EntityLeaveWorldEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player)
    }

    override fun onStart(context: ScriptContext, event: EntityLeaveWorldEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["entityUUID"] = event.entityUUID
        val bukkit = Bukkit.getEntity(event.entityUUID)?.abstract()
        val ady = if (AdyeshachPlugin.isEnabled) {
            Adyeshach.api().getEntityFinder().getEntityFromUniqueId(FastUUID.toString(event.entityUUID), event.player)?.abstract()
        } else {
            null
        }
        context["entity"] = bukkit ?: ady
    }

}
package org.gitee.orryx.core.station.triggers.arcartx

import com.eatthepath.uuid.FastUUID
import ink.ptms.adyeshach.core.Adyeshach
import org.bukkit.Bukkit
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.AdyeshachPlugin
import org.gitee.orryx.utils.abstract
import priv.seventeen.artist.arcartx.event.client.ClientEntityLeaveEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("ArcartX")
object ArcartXEntityLeaveTrigger: AbstractPropertyEventTrigger<ClientEntityLeaveEvent>("ArcartX Entity Leave") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.ARCARTX, event)
            .addParm(Type.STRING, "uuid", "实体的UUID")
            .addParm(Type.TARGET, "entity", "实体，不一定存在")
            .description("玩家客户端中有实体离开")

    override val clazz
        get() = ClientEntityLeaveEvent::class.java

    override fun onJoin(event: ClientEntityLeaveEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: ClientEntityLeaveEvent, map: Map<String, Any?>): Boolean {
        return (pipeTask.scriptContext?.sender?.origin == event.player)
    }

    override fun read(instance: ClientEntityLeaveEvent, key: String): OpenResult {
        return when(key) {
            "uuid" -> OpenResult.successful(instance.entityUUID)
            "entity" -> {
                val bukkit = Bukkit.getEntity(instance.entityUUID)?.abstract()
                val ady = if (AdyeshachPlugin.isEnabled) {
                    Adyeshach.api().getEntityFinder().getEntityFromUniqueId(FastUUID.toString(instance.entityUUID), instance.player)?.abstract()
                } else {
                    null
                }
                OpenResult.successful(bukkit ?: ady)
            }
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: ClientEntityLeaveEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

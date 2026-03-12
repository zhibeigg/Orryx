package org.gitee.orryx.core.station.triggers.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import org.bukkit.entity.Player
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("MythicMobs")
object MythicMobDeathTrigger: AbstractPropertyEventTrigger<MythicMobDeathEvent>("MythicMob Death") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.MYTHIC_MOBS, event)
            .addParm(Type.STRING, "mobType", "MythicMob类型名")
            .addParm(Type.DOUBLE, "mobLevel", "MythicMob等级")
            .addParm(Type.TARGET, "entity", "死亡的实体")
            .addParm(Type.TARGET, "killer", "击杀者")
            .description("当玩家击杀MythicMob时触发")

    override val clazz
        get() = MythicMobDeathEvent::class.java

    override fun onCheck(station: IStation, event: MythicMobDeathEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.killer is Player
    }

    override fun onJoin(event: MythicMobDeathEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.killer as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: MythicMobDeathEvent, map: Map<String, Any?>): Boolean {
        return event.killer is Player && pipeTask.scriptContext?.sender?.origin == event.killer
    }

    override fun read(instance: MythicMobDeathEvent, key: String): OpenResult {
        return when(key) {
            "mobType" -> OpenResult.successful(instance.mobType.internalName)
            "mobLevel" -> OpenResult.successful(instance.mobLevel)
            "entity" -> OpenResult.successful(instance.entity.abstract())
            "killer" -> OpenResult.successful(instance.killer.abstract())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: MythicMobDeathEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}

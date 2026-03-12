package org.gitee.orryx.core.station.triggers.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobLootDropEvent
import org.bukkit.entity.Player
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.stations.IStation
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.common5.cint

@Plugin("MythicMobs")
object MythicMobLootDropTrigger: AbstractPropertyEventTrigger<MythicMobLootDropEvent>("MythicMob Loot Drop") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.MYTHIC_MOBS, event)
            .addParm(Type.STRING, "mobType", "MythicMob类型名")
            .addParm(Type.DOUBLE, "mobLevel", "MythicMob等级")
            .addParm(Type.TARGET, "killer", "击杀者")
            .addParm(Type.INT, "exp", "掉落经验")
            .addParm(Type.INT, "money", "掉落金币")
            .description("当MythicMob掉落战利品时触发（仅玩家击杀）")

    override val clazz
        get() = MythicMobLootDropEvent::class.java

    override fun onCheck(station: IStation, event: MythicMobLootDropEvent, map: Map<String, Any?>): Boolean {
        return super.onCheck(station, event, map) && event.killer is Player
    }

    override fun onJoin(event: MythicMobLootDropEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.killer as Player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: MythicMobLootDropEvent, map: Map<String, Any?>): Boolean {
        return event.killer is Player && pipeTask.scriptContext?.sender?.origin == event.killer
    }

    override fun read(instance: MythicMobLootDropEvent, key: String): OpenResult {
        return when(key) {
            "mobType" -> OpenResult.successful(instance.mobType.internalName)
            "mobLevel" -> OpenResult.successful(instance.mobLevel)
            "killer" -> OpenResult.successful(instance.killer.abstract())
            "exp" -> OpenResult.successful(instance.exp)
            "money" -> OpenResult.successful(instance.money)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: MythicMobLootDropEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "exp" -> {
                instance.exp = value.cint
                OpenResult.successful()
            }
            "money" -> {
                instance.money = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

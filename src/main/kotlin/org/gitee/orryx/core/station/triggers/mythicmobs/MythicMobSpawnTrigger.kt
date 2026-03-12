package org.gitee.orryx.core.station.triggers.mythicmobs

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptCommandSender

@Plugin("MythicMobs")
object MythicMobSpawnTrigger: AbstractPropertyEventTrigger<MythicMobSpawnEvent>("MythicMob Spawn") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.MYTHIC_MOBS, event)
            .addParm(Type.STRING, "mobType", "MythicMob类型名")
            .addParm(Type.DOUBLE, "mobLevel", "MythicMob等级")
            .addParm(Type.TARGET, "entity", "生成的实体")
            .addParm(Type.TARGET, "location", "生成位置")
            .description("当MythicMob生成时触发")

    override val clazz
        get() = MythicMobSpawnEvent::class.java

    override fun onJoin(event: MythicMobSpawnEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptCommandSender(org.bukkit.Bukkit.getConsoleSender())
    }

    override fun onCheck(pipeTask: IPipeTask, event: MythicMobSpawnEvent, map: Map<String, Any?>): Boolean {
        return true
    }

    override fun read(instance: MythicMobSpawnEvent, key: String): OpenResult {
        return when(key) {
            "mobType" -> OpenResult.successful(instance.mobType.internalName)
            "mobLevel" -> OpenResult.successful(instance.mobLevel)
            "entity" -> OpenResult.successful(instance.entity.abstract())
            "location" -> OpenResult.successful(instance.location.toTarget())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: MythicMobSpawnEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "mobLevel" -> {
                instance.mobLevel = (value as? Number)?.toDouble() ?: return OpenResult.failed()
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

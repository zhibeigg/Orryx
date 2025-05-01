package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.core.targets.ITargetLocation
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult
import taboolib.common5.cbool
import taboolib.common5.cint
import taboolib.module.kether.ScriptContext

object PlayerPortalTrigger: AbstractPropertyPlayerEventTrigger<PlayerPortalEvent>("Player Portal") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.BOOLEAN, "canCreatePortal", "返回服务器是否尝试创建目标传送门")
            .addParm(Type.INT, "searchRadius", "获取查找可用门户的搜索半径值")
            .addParm(Type.INT, "creationRadius", "获取从给定位置搜索世界可用空间的最大半径")
            .addParm(Type.TARGET, "from", "来的位置")
            .addParm(Type.TARGET, "to", "到的位置")
            .description("玩家将要被传送门传送触发, 传送过程中会生成一个退出传送门")

    override val clazz
        get() = PlayerPortalEvent::class.java

    override fun read(instance: PlayerPortalEvent, key: String): OpenResult {
        return when(key) {
            "canCreatePortal" -> OpenResult.successful(instance.canCreatePortal)
            "searchRadius" -> OpenResult.successful(instance.searchRadius)
            "creationRadius" -> OpenResult.successful(instance.creationRadius)
            "from" -> OpenResult.successful(instance.from.toTarget())
            "to" -> OpenResult.successful(instance.to?.toTarget())
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerPortalEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "canCreatePortal" -> {
                instance.canCreatePortal = value.cbool
                OpenResult.successful()
            }
            "searchRadius" -> {
                instance.searchRadius = value.cint
                OpenResult.successful()
            }
            "creationRadius" -> {
                instance.creationRadius = value.cint
                OpenResult.successful()
            }
            "from" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.from = it }
                OpenResult.successful()
            }
            "to" -> {
                (value as? ITargetLocation<*>)?.location?.let { instance.setTo(it) }
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
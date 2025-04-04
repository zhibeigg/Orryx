package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerPortalEvent
import org.gitee.orryx.core.station.triggers.AbstractPlayerEventTrigger
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerPortalTrigger: AbstractPlayerEventTrigger<PlayerPortalEvent>() {

    override val event: String = "Player Portal"

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

    override fun onStart(context: ScriptContext, event: PlayerPortalEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["canCreatePortal"] = event.canCreatePortal
        context["searchRadius"] = event.searchRadius
        context["creationRadius"] = event.creationRadius
        context["from"] = LocationTarget(event.from)
        context["to"] = event.to?.let { LocationTarget(it) }
    }

}
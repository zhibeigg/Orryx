package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEggThrowEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerEggThrowTrigger: AbstractPlayerEventTrigger<PlayerEggThrowEvent>() {

    override val event: String = "Player Egg Throw"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "egg", "鸡蛋实体")
            .addParm(Type.BOOLEAN, "isHatching", "检测鸡蛋是否将被孵化")
            .addParm(Type.BYTE, "numHatches", "检测将被孵化生物的数量")
            .addParm(Type.STRING, "hatchingType", "获取将被孵化的生物类型(默认为EntityType.CHICKEN)")
            .description("玩家抛出鸡蛋时触发本事件，鸡蛋可能孵化。")

    override val clazz
        get() = PlayerEggThrowEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerEggThrowEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["egg"] = AbstractBukkitEntity(event.egg)
        context["isHatching"] = event.isHatching
        context["numHatches"] = event.numHatches
        context["hatchingType"] = event.hatchingType.name
    }

}
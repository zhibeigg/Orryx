package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerFishEvent
import org.gitee.orryx.api.adapters.entity.AbstractBukkitEntity
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.module.kether.ScriptContext

object PlayerFishTrigger: AbstractPlayerEventTrigger<PlayerFishEvent>() {

    override val event: String = "Player Exp Cooldown Change"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "state", "钓鱼状态：BITE/CAUGHT_ENTITY/CAUGHT_FISH/FAILED_ATTEMPT/FISHING/IN_GROUND/REEL_IN")
            .addParm(Type.STRING, "hand", "获取此事件中使用的手：OFF_HAND/HAND")
            .addParm(Type.STRING, "hookState", "获取此鱼钩的当前状态：BOBBING/HOOKED_ENTITY/UNHOOKED")
            .addParm(Type.TARGET, "hookedEntity", "被钩中的实体")
            .addParm(Type.BOOLEAN, "applyLure", "获取是否应应用诱饵附魔来减少等待时间")
            .addParm(Type.BOOLEAN, "isInOpenWater", "检查这个鱼钩是否在开阔的水域中")
            .addParm(Type.BOOLEAN, "isRainInfluenced", "等待和诱饵的时间是否会受到雨水的影响")
            .addParm(Type.BOOLEAN, "isSkyInfluenced", "等待和诱饵的时间是否会受到直达天空的影响")
            .addParm(Type.FLOAT, "maxLureAngle", "获取等待时间之后鱼出现的最大角度")
            .addParm(Type.FLOAT, "minLureAngle", "获取等待时间之后鱼出现的最小角度")
            .addParm(Type.INT, "maxLureTime", "获得鱼出现后等待鱼上钩所需等待的最大Tick数")
            .addParm(Type.INT, "minLureTime", "获得鱼出现后等待鱼上钩所需等待的最小Tick数")
            .addParm(Type.INT, "maxWaitTime", "获取等待鱼出现的最大Tick数")
            .addParm(Type.INT, "minWaitTime", "获取等待鱼出现的最小Tick数")
            .addParm(Type.DOUBLE, "biteChance", "咬钩几率")
            .description("当玩家经验冷却时间发生变化时触发")

    override val clazz
        get() = PlayerFishEvent::class.java

    override fun onStart(context: ScriptContext, event: PlayerFishEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["state"] = event.state.name
        context["hand"] = event.hand?.name
        context["hookState"] = event.hook.state.name
        context["hookedEntity"] = event.hook.hookedEntity?.let { AbstractBukkitEntity(it) }
        context["applyLure"] = event.hook.applyLure
        context["isInOpenWater"] = event.hook.isInOpenWater
        context["isRainInfluenced"] = event.hook.isRainInfluenced
        context["isSkyInfluenced"] = event.hook.isSkyInfluenced
        context["maxLureAngle"] = event.hook.maxLureAngle
        context["minLureAngle"] = event.hook.minLureAngle
        context["maxLureTime"] = event.hook.maxLureTime
        context["minLureTime"] = event.hook.minLureTime
        context["maxWaitTime"] = event.hook.maxWaitTime
        context["minWaitTime"] = event.hook.minWaitTime
        context["biteChance"] = event.hook.biteChance
    }

}
package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerFishEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult
import taboolib.common5.cbool
import taboolib.common5.cdouble
import taboolib.common5.cfloat
import taboolib.common5.cint

object PlayerFishTrigger: AbstractPropertyPlayerEventTrigger<PlayerFishEvent>("Player Exp Cooldown Change") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "status", "钓鱼状态：BITE/CAUGHT_ENTITY/CAUGHT_FISH/FAILED_ATTEMPT/FISHING/IN_GROUND/REEL_IN")
            .addParm(Type.STRING, "hand", "获取此事件中使用的手：OFF_HAND/HAND")
            .addParm(Type.TARGET, "caught", "玩家捕获的实体")
            .addParm(Type.TARGET, "expToDrop", "掉落的经验")
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

    override val clazz: java
        get() = PlayerFishEvent::class.java

    override fun read(instance: PlayerFishEvent, key: String): OpenResult {
        return when(key) {
            "status" -> OpenResult.successful(instance.state.name)
            "hand" -> OpenResult.successful(instance.hand?.name)
            "caught" -> OpenResult.successful(instance.caught?.abstract())
            "expToDrop" -> OpenResult.successful(instance.expToDrop)
            "hookState" -> OpenResult.successful(instance.hook.state.name)
            "hookedEntity" -> OpenResult.successful(instance.hook.hookedEntity?.abstract())
            "applyLure" -> OpenResult.successful(instance.hook.applyLure)
            "isInOpenWater" -> OpenResult.successful(instance.hook.isInOpenWater)
            "isRainInfluenced" -> OpenResult.successful(instance.hook.isRainInfluenced)
            "isSkyInfluenced" -> OpenResult.successful(instance.hook.isSkyInfluenced)
            "maxLureAngle" -> OpenResult.successful(instance.hook.maxLureAngle)
            "minLureAngle" -> OpenResult.successful(instance.hook.minLureAngle)
            "maxLureTime" -> OpenResult.successful(instance.hook.maxLureTime)
            "minLureTime" -> OpenResult.successful(instance.hook.minLureTime)
            "maxWaitTime" -> OpenResult.successful(instance.hook.maxWaitTime)
            "minWaitTime" -> OpenResult.successful(instance.hook.minWaitTime)
            "biteChance" -> OpenResult.successful(instance.hook.biteChance)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerFishEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "status" -> {
                instance.expToDrop = value.cint
                OpenResult.successful()
            }
            "applyLure" -> {
                instance.hook.applyLure = value.cbool
                OpenResult.successful()
            }
            "isRainInfluenced" -> {
                instance.hook.isRainInfluenced = value.cbool
                OpenResult.successful()
            }
            "isSkyInfluenced" -> {
                instance.hook.isSkyInfluenced = value.cbool
                OpenResult.successful()
            }
            "maxLureAngle" -> {
                instance.hook.maxLureAngle = value.cfloat
                OpenResult.successful()
            }
            "minLureAngle" -> {
                instance.hook.minLureAngle = value.cfloat
                OpenResult.successful()
            }
            "maxLureTime" -> {
                instance.hook.maxLureTime = value.cint
                OpenResult.successful()
            }
            "minLureTime" -> {
                instance.hook.minLureTime = value.cint
                OpenResult.successful()
            }
            "maxWaitTime" -> {
                instance.hook.maxWaitTime = value.cint
                OpenResult.successful()
            }
            "minWaitTime" -> {
                instance.hook.minWaitTime = value.cint
                OpenResult.successful()
            }
            "biteChance" -> {
                instance.hook.biteChance = value.cdouble
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
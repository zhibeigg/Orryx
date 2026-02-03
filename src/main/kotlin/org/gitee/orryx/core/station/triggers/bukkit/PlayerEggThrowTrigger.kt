package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerEggThrowEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.abstract
import taboolib.common.OpenResult
import taboolib.common5.cbool
import taboolib.common5.cbyte
import taboolib.library.xseries.XEntityType

object PlayerEggThrowTrigger: AbstractPropertyPlayerEventTrigger<PlayerEggThrowEvent>("Player Egg Throw") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.TARGET, "egg", "鸡蛋实体")
            .addParm(Type.BOOLEAN, "isHatching", "检测鸡蛋是否将被孵化")
            .addParm(Type.BYTE, "numHatches", "检测将被孵化生物的数量")
            .addParm(Type.STRING, "hatchingType", "获取将被孵化的生物类型(默认为EntityType.CHICKEN)")
            .description("玩家抛出鸡蛋时触发本事件，鸡蛋可能孵化。")

    override val clazz
        get() = PlayerEggThrowEvent::class.java

    override fun read(instance: PlayerEggThrowEvent, key: String): OpenResult {
        return when(key) {
            "egg" -> OpenResult.successful(instance.egg.abstract())
            "isHatching" -> OpenResult.successful(instance.isHatching)
            "numHatches" -> OpenResult.successful(instance.numHatches)
            "hatchingType" -> OpenResult.successful(instance.hatchingType.name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerEggThrowEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "isHatching" -> {
                instance.isHatching = value.cbool
                OpenResult.successful()
            }
            "numHatches" -> {
                instance.numHatches = value.cbyte
                OpenResult.successful()
            }
            "hatchingType" -> {
                XEntityType.of(value.toString())?.get()?.get()?.let {
                    instance.hatchingType = it
                }
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}
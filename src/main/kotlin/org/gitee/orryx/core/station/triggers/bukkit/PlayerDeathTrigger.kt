package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.entity.PlayerDeathEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import taboolib.common.OpenResult
import taboolib.common5.cbool
import taboolib.common5.cint

object PlayerDeathTrigger: AbstractPropertyPlayerEventTrigger<PlayerDeathEvent>("Player Death") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "deathMessage", "死亡消息")
            .addParm(Type.BOOLEAN, "keepInventory", "是否保留背包")
            .addParm(Type.BOOLEAN, "keepLevel", "是否保留等级")
            .addParm(Type.INT, "newLevel", "重生后的等级")
            .addParm(Type.INT, "newTotalExp", "重生后的总经验")
            .addParm(Type.INT, "newExp", "重生后的经验")
            .addParm(Type.INT, "droppedExp", "掉落的经验")
            .description("当玩家死亡时触发")

    override val clazz
        get() = PlayerDeathEvent::class.java

    override fun read(instance: PlayerDeathEvent, key: String): OpenResult {
        return when(key) {
            "deathMessage" -> OpenResult.successful(instance.deathMessage)
            "keepInventory" -> OpenResult.successful(instance.keepInventory)
            "keepLevel" -> OpenResult.successful(instance.keepLevel)
            "newLevel" -> OpenResult.successful(instance.newLevel)
            "newTotalExp" -> OpenResult.successful(instance.newTotalExp)
            "newExp" -> OpenResult.successful(instance.newExp)
            "droppedExp" -> OpenResult.successful(instance.droppedExp)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerDeathEvent, key: String, value: Any?): OpenResult {
        return when(key) {
            "deathMessage" -> {
                instance.deathMessage = value?.toString()
                OpenResult.successful()
            }
            "keepInventory" -> {
                instance.keepInventory = value.cbool
                OpenResult.successful()
            }
            "keepLevel" -> {
                instance.keepLevel = value.cbool
                OpenResult.successful()
            }
            "newLevel" -> {
                instance.newLevel = value.cint
                OpenResult.successful()
            }
            "newTotalExp" -> {
                instance.newTotalExp = value.cint
                OpenResult.successful()
            }
            "newExp" -> {
                instance.newExp = value.cint
                OpenResult.successful()
            }
            "droppedExp" -> {
                instance.droppedExp = value.cint
                OpenResult.successful()
            }
            else -> OpenResult.failed()
        }
    }
}

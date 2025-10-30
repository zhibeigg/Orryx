package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBedEnterEvent
import org.gitee.orryx.core.station.triggers.AbstractPropertyPlayerEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.gitee.orryx.utils.toTarget
import taboolib.common.OpenResult

object PlayerBedEnterTrigger: AbstractPropertyPlayerEventTrigger<PlayerBedEnterEvent>("Player Bed Enter") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "bedEnterResult", "玩家进入床的结果：NOT_POSSIBLE_HERE/NOT_POSSIBLE_NOW/NOT_SAFE/OK/OTHER_PROBLEM/TOO_FAR_AWAY")
            .addParm(Type.TARGET, "bed", "床的位置")
            .addParm(Type.STRING, "useBed", "玩家使用床的结果：ALLOW/DEFAULT/DENY")
            .description("玩家准备躺到床上时触发")

    override val clazz: java
        get() = PlayerBedEnterEvent::class.java

    override fun read(instance: PlayerBedEnterEvent, key: String): OpenResult {
        return when(key) {
            "bedEnterResult" -> OpenResult.successful(instance.bedEnterResult.name)
            "bed" -> OpenResult.successful(instance.bed.location.toTarget())
            "useBed" -> OpenResult.successful(instance.useBed().name)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerBedEnterEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
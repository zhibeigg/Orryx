package org.gitee.orryx.core.station.triggers.bukkit

import org.bukkit.event.player.PlayerBedEnterEvent
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.targets.LocationTarget
import org.gitee.orryx.core.wiki.Trigger
import org.gitee.orryx.core.wiki.TriggerGroup
import org.gitee.orryx.core.wiki.Type
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.module.kether.ScriptContext

object PlayerBedEnterTrigger: AbstractEventTrigger<PlayerBedEnterEvent>() {

    override val event: String = "Player Bed Enter"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.BUKKIT, event)
            .addParm(Type.STRING, "bedEnterResult", "玩家进入床的结果：NOT_POSSIBLE_HERE/NOT_POSSIBLE_NOW/NOT_SAFE/OK/OTHER_PROBLEM/TOO_FAR_AWAY")
            .addParm(Type.TARGET, "bed", "床的位置")
            .addParm(Type.STRING, "useBed", "玩家使用床的结果：ALLOW/DEFAULT/DENY")
            .description("玩家准备躺到床上时触发")

    override val clazz
        get() = PlayerBedEnterEvent::class.java

    override fun onJoin(event: PlayerBedEnterEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun onCheck(pipeTask: IPipeTask, event: PlayerBedEnterEvent, map: Map<String, Any?>): Boolean {
        return pipeTask.scriptContext?.sender?.origin == event.player
    }

    override fun onStart(context: ScriptContext, event: PlayerBedEnterEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["bedEnterResult"] = event.bedEnterResult.name
        context["bed"] = LocationTarget(event.bed.location)
        context["useBed"] = event.useBed().name
    }

}
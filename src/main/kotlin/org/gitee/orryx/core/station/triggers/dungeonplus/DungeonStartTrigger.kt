package org.gitee.orryx.core.station.triggers.dungeonplus

import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPipeEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent
import org.serverct.ersha.dungeon.common.api.event.dungeon.DungeonStartEvent
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import taboolib.module.kether.ScriptContext

@Plugin("DungeonPlus")
object DungeonStartTrigger: AbstractPipeEventTrigger<DungeonEvent>() {

    override val event = "Dungeon Start"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "dungeonName", "副本名")
            .addParm(Type.STRING, "dungeonUUID", "副本UUID")
            .addParm(Type.ITERABLE, "params", "副本参数")
            .description("副本启动时触发（仅Pipe）")

    override val clazz
        get() = DungeonEvent::class.java

    override fun onCheck(pipeTask: IPipeTask, event: DungeonEvent, map: Map<String, Any?>): Boolean {
        val e = event.event
        return e is DungeonStartEvent.After && (pipeTask.scriptContext?.sender?.origin in e.dungeon.team.getPlayers(PlayerStateType.ONLINE))
    }

    override fun onStart(context: ScriptContext, event: DungeonEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["dungeonName"] = event.dungeon.dungeonName
        context["dungeonUUID"] = event.dungeon.dungeonUuid.toString()
        context["params"] = event.dungeon.params
    }

}
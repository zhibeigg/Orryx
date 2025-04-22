package org.gitee.orryx.core.station.triggers.dungeonplus

import com.eatthepath.uuid.FastUUID
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPipeEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent
import org.serverct.ersha.dungeon.common.api.event.dungeon.DungeonEndEvent
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import taboolib.module.kether.ScriptContext

@Plugin("DungeonPlus")
object DungeonEndTrigger: AbstractPipeEventTrigger<DungeonEvent>() {

    override val event = "Dungeon End"

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "dungeonName", "副本名")
            .addParm(Type.STRING, "dungeonUUID", "副本UUID")
            .addParm(Type.ITERABLE, "params", "副本参数")
            .description("副本结束时触发（仅Pipe）")

    override val clazz
        get() = DungeonEvent::class.java

    override fun onCheck(pipeTask: IPipeTask, event: DungeonEvent, map: Map<String, Any?>): Boolean {
        val e = event.event
        return e is DungeonEndEvent.After && (pipeTask.scriptContext?.sender?.origin in e.dungeon.team.getPlayers(PlayerStateType.ONLINE))
    }

    override fun onStart(context: ScriptContext, event: DungeonEvent, map: Map<String, Any?>) {
        super.onStart(context, event, map)
        context["dungeonName"] = event.dungeon.dungeonName
        context["dungeonUUID"] = FastUUID.toString(event.dungeon.dungeonUuid)
        context["params"] = event.dungeon.params
    }

}
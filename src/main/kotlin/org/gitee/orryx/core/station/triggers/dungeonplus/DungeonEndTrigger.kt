package org.gitee.orryx.core.station.triggers.dungeonplus

import com.eatthepath.uuid.FastUUID
import org.gitee.orryx.api.events.compat.PlayerDungeonEvent
import org.gitee.orryx.core.station.Plugin
import org.gitee.orryx.core.station.pipe.IPipeTask
import org.gitee.orryx.core.station.triggers.AbstractPropertyEventTrigger
import org.gitee.orryx.module.wiki.Trigger
import org.gitee.orryx.module.wiki.TriggerGroup
import org.gitee.orryx.module.wiki.Type
import org.serverct.ersha.dungeon.common.api.event.dungeon.DungeonEndEvent
import taboolib.common.OpenResult
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.adaptPlayer

@Plugin("DungeonPlus")
object DungeonEndTrigger: AbstractPropertyEventTrigger<PlayerDungeonEvent>("Dungeon End") {

    override val wiki: Trigger
        get() = Trigger.new(TriggerGroup.DRAGONCORE, event)
            .addParm(Type.STRING, "dungeonName", "副本名")
            .addParm(Type.STRING, "dungeonUUID", "副本UUID")
            .addParm(Type.ITERABLE, "params", "副本参数")
            .description("副本结束时触发")

    override val clazz
        get() = PlayerDungeonEvent::class.java

    override fun onCheck(pipeTask: IPipeTask, event: PlayerDungeonEvent, map: Map<String, Any?>): Boolean {
        val e = event.event.event
        return e is DungeonEndEvent.After && (pipeTask.scriptContext?.sender?.origin == event.player)
    }

    override fun onJoin(event: PlayerDungeonEvent, map: Map<String, Any?>): ProxyCommandSender {
        return adaptPlayer(event.player)
    }

    override fun read(instance: PlayerDungeonEvent, key: String): OpenResult {
        return when(key) {
            "dungeonName" -> OpenResult.successful(instance.event.dungeon.dungeonName)
            "dungeonUUID" -> OpenResult.successful(FastUUID.toString(instance.event.dungeon.dungeonUuid))
            "params" -> OpenResult.successful(instance.event.dungeon.params)
            else -> OpenResult.failed()
        }
    }

    override fun write(instance: PlayerDungeonEvent, key: String, value: Any?): OpenResult {
        return OpenResult.failed()
    }
}
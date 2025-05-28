package org.gitee.orryx.core.station.triggers.dungeonplus

import org.gitee.orryx.api.events.compat.PlayerDungeonEvent
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType
import taboolib.common.platform.Ghost
import taboolib.common.platform.event.SubscribeEvent

object DungeonEventProxy {

    @Ghost
    @SubscribeEvent
    fun proxy(e: DungeonEvent) {
        if (e.isCancelled) return
        e.dungeon.team.getPlayers(state = PlayerStateType.ONLINE).forEach {
            PlayerDungeonEvent(it, e).call()
        }
    }
}
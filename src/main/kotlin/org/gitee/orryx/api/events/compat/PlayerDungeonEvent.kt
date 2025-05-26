package org.gitee.orryx.api.events.compat

import org.bukkit.entity.Player
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent
import taboolib.platform.type.BukkitProxyEvent

class PlayerDungeonEvent(val player: Player, val event: DungeonEvent): BukkitProxyEvent()
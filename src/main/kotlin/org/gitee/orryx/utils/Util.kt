package org.gitee.orryx.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.common5.clong

fun String?.toIntPair(vararg delimiters: String): Pair<Int, Int> {
    this ?: return Pair(0, 0)
    return split(*delimiters).let {
        it[0].cint to it[1].cint
    }
}
fun String?.toLongPair(vararg delimiters: String): Pair<Long, Long> {
    this ?: return Pair(0L, 0L)
    return split(*delimiters).let {
        it[0].clong to it[1].clong
    }
}

fun getNearPlayers(entity: Entity): List<Player> {
    return entity.world.players
}

fun getNearPlayers(entity: Entity, func: (Player) -> Unit) {
    entity.world.players.forEach(func)
}
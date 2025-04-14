package org.gitee.orryx.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.common5.clong
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun String?.toIntPair(vararg delimiters: String): Pair<Int, Int> {
    this ?: return Pair(0, 0)
    return split(*delimiters).let {
        it[0].cint to it[1].cint
    }
}
internal fun String?.toLongPair(vararg delimiters: String): Pair<Long, Long> {
    this ?: return Pair(0L, 0L)
    return split(*delimiters).let {
        it[0].clong to it[1].clong
    }
}

internal fun getNearPlayers(entity: Entity): List<Player> {
    return entity.world.players
}

internal fun getNearPlayers(entity: Entity, func: (Player) -> Unit) {
    entity.world.players.forEach(func)
}

class ReloadableLazy<T>(private val check: () -> Any?, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var cached: T? = null
    private var initialized: Boolean = false
    private var lastHash: Int? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val current = check()
        val currentHash = current.hashCode()
        if (!initialized || lastHash != currentHash) {
            cached = initializer()
            initialized = true
            lastHash = currentHash
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
}
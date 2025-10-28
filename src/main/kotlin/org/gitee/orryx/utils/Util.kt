package org.gitee.orryx.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import taboolib.common5.cint
import taboolib.common5.clong
import taboolib.module.configuration.Configuration
import kotlin.math.pow
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

internal fun String.clearSpace(): String {
    return replace(" ", "")
}

internal fun getNearPlayers(entity: Entity): List<Player> {
    return entity.world.players
}

internal inline fun getNearPlayers(entity: Entity, func: (Player) -> Unit) {
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

class ConfigLazy<T>(val config: Configuration, private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private var cached: T? = null
    private var initialized: Boolean = false
    private var lastHash: Int? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val currentHash = config.file?.readBytes().hashCode()
        if (!initialized || lastHash != currentHash) {
            cached = initializer()
            initialized = true
            lastHash = currentHash
        }
        @Suppress("UNCHECKED_CAST")
        return cached as T
    }
}

class RangedDouble(value: String, squared: Boolean = false) {

    val operation: Operation

    var min: Double = 0.0
        private set

    var max: Double = 0.0
        private set

    init {
        if (value.contains("to")) {
            val split: Array<String?> = value.split("to".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            this.min = split[0]!!.toDouble()
            this.max = split[1]!!.toDouble()
            this.operation = Operation.RANGE
        } else if (!value.startsWith("-") && value.contains("-")) {
            val split: Array<String?> = value.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            this.min = split[0]!!.toInt().toDouble()
            this.max = split[1]!!.toInt().toDouble()
            this.operation = Operation.RANGE
        } else if (value.startsWith(">")) {
            val s = value.substring(1)
            this.min = s.toDouble()
            this.max = Double.Companion.MAX_VALUE
            this.operation = Operation.GREATER_THAN
        } else if (value.startsWith("<")) {
            val s = value.substring(1)
            this.min = Double.Companion.MIN_VALUE
            this.max = s.toDouble()
            this.operation = Operation.LESS_THAN
        } else {
            this.min = value.toDouble()
            this.max = value.toDouble()
            this.operation = Operation.EQUALS
        }

        if (squared) {
            this.min *= this.min
            this.max *= this.max
        }
    }

    override fun equals(other: Any?): Boolean {
        if ((other !is Int) && (other !is Double) && (other !is Float)) {
            return false
        } else {
            val d: Double = when (other) {
                is Int -> {
                    other.toDouble() * 1.0
                }

                is Float -> {
                    other.toDouble() * 1.0
                }

                else -> {
                    other as Double
                }
            }

            return when (this.operation) {
                Operation.EQUALS -> d == this.min
                Operation.GREATER_THAN -> d > this.min
                Operation.LESS_THAN -> d < this.max
                Operation.RANGE -> d >= this.min && d <= this.max
            }
        }
    }

    fun equalsSquared(o: Any?): Boolean {
        if ((o !is Int) && (o !is Double) && (o !is Float)) {
            return false
        } else {
            val d: Double = when (o) {
                is Int -> {
                    o.toDouble() * 1.0
                }

                is Float -> {
                    o.toDouble() * 1.0
                }

                else -> {
                    o as Double
                }
            }

            return when (this.operation) {
                Operation.EQUALS -> d == this.min.pow(2.0)
                Operation.GREATER_THAN -> d > this.min.pow(2.0)
                Operation.LESS_THAN -> d < this.max.pow(2.0)
                Operation.RANGE -> d >= this.min.pow(2.0) && d <= this.max.pow(2.0)
            }
        }
    }

    override fun toString(): String {
        return "RangedDouble{" + this.min + " to " + this.max + "}"
    }

    enum class Operation {
        EQUALS,
        GREATER_THAN,
        LESS_THAN,
        RANGE
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        result = 31 * result + operation.hashCode()
        return result
    }
}

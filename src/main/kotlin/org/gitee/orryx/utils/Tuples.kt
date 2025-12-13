package org.gitee.orryx.utils

import kotlinx.serialization.Serializable

@Serializable
data class Pair<out A, out B>(
    val first: A,
    val second: B
) {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second)"
}

infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)

@Serializable
data class Triple<out A, out B, out C>(
    val first: A,
    val second: B,
    val third: C
) {

    /**
     * Returns string representation of the [Triple] including its [first], [second] and [third] values.
     */
    override fun toString(): String = "($first, $second, $third)"
}

fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)